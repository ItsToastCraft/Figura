package net.blancworks.figura.avatar;

import net.blancworks.figura.FiguraMod;
import net.minecraft.nbt.*;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

public class LocalAvatarManager {

    public static final Map<String, Boolean> FOLDER_DATA = new HashMap<>();
    public static final Map<String, LocalAvatar> AVATARS = new LinkedHashMap<>();
    public static boolean init = false;

    private static final String RESOURCE_FOLDER_NAME = "[§9Figura§r] Resource Avatars";
    public static final LocalAvatarFolder RESOURCE_FOLDER = new LocalAvatarFolder(RESOURCE_FOLDER_NAME, true);

    public static void init() {
        loadFolderNbt();
        loadFromDisk();
    }

    public static void loadFolderNbt() {
        try {
            //io
            Path targetPath = FiguraMod.getModContentDirectory().resolve("model_folders.nbt");

            if (!Files.exists(targetPath))
                return;

            FileInputStream fis = new FileInputStream(targetPath.toFile());
            NbtCompound nbt = NbtIo.readCompressed(fis);

            //loading
            NbtList groupList = nbt.getList("folders", NbtElement.COMPOUND_TYPE);
            for (NbtElement value : groupList) {
                NbtCompound compound = (NbtCompound) value;

                String path = compound.getString("path");
                boolean expanded = compound.getBoolean("expanded");
                FOLDER_DATA.put(path, expanded);
            }

            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateResourceExpanded(RESOURCE_FOLDER);
    }

    public static void saveFolderNbt() {
        try {
            //writing
            NbtCompound nbt = new NbtCompound();
            NbtList folderList = new NbtList();

            FOLDER_DATA.forEach((key, value) -> {
                if (!value) {
                    NbtCompound container = new NbtCompound();
                    container.put("path", NbtString.of(key));
                    container.put("expanded", NbtByte.of(false));

                    folderList.add(container);
                }
            });

            nbt.put("folders", folderList);

            //io
            Path targetPath = FiguraMod.getModContentDirectory();
            targetPath = targetPath.resolve("model_folders.nbt");

            if (!Files.exists(targetPath))
                Files.createFile(targetPath);

            FileOutputStream fs = new FileOutputStream(targetPath.toFile());
            NbtIo.writeCompressed(nbt, fs);

            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadFromDisk() {
        //reload avatars
        FiguraMod.doTask(() -> {
            Path contentDirectory = LocalAvatarData.getContentDirectory();

            try {
                if (!Files.exists(contentDirectory))
                    Files.createDirectories(contentDirectory);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                loadAvatars(contentDirectory.toFile(), AVATARS);
                init = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void loadAvatars(File contentDirectory, Map<String, LocalAvatar> parent) {
        File[] files = contentDirectory.listFiles();

        if (files == null || parent == null)
            return;

        Map<String, LocalAvatar> newParent = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //for all files in folder
        for (File file : files) {
            String path = file.getAbsolutePath();
            boolean added = parent.containsKey(path);

            if (!added) {
                if (hasAvatar(file)) {
                    //add file if has avatar
                    newParent.put(path, new LocalAvatar(file.getName()));
                } else if (file.isDirectory()) {
                    //load directory avatars
                    LocalAvatarFolder folder = new LocalAvatarFolder(file.getName(), !FOLDER_DATA.containsKey(path) || FOLDER_DATA.get(path));
                    loadAvatars(file, folder.children);

                    //do not add if dir is empty
                    if (!folder.children.isEmpty()) newParent.put(path, folder);
                }
            } else {
                LocalAvatar entry = parent.get(path);

                if (entry instanceof LocalAvatarFolder folderEntry) {
                    //load avatars from subfolder
                    loadAvatars(file, folderEntry.children);

                    //do not add empty subfolder
                    if (!folderEntry.children.isEmpty()) newParent.put(path, folderEntry);
                } else {
                    //do not add invalid loaded avatars
                    if (hasAvatar(file)) newParent.put(path, entry);
                }
            }
        }

        parent.clear();

        if (parent == AVATARS && !RESOURCE_FOLDER.children.isEmpty())
            AVATARS.put(RESOURCE_FOLDER_NAME, RESOURCE_FOLDER);

        //sort folders
        sortFolders(newParent, parent);
    }

    private static boolean hasAvatar(File file) {
        try {
            byte load = 0;

            //moon (figura avatar data) load
            if (file.getName().endsWith(".moon")) {
                return true;
            }
            //zip load
            else if (file.getName().endsWith(".zip")) {
                ZipFile zipFile = new ZipFile(file.getPath());

                if (zipFile.getEntry("model.bbmodel") != null) load = (byte) (load | 1);
                else if (zipFile.getEntry("player_model.bbmodel") != null) load = (byte) (load | 2);
                else if (zipFile.getEntry("script.lua") != null) load = (byte) (load | 4);
            }
            //directory load
            else if (file.isDirectory()) {
                if (Files.exists(file.toPath().resolve("model.bbmodel"))) load = (byte) (load | 1);
                else if (Files.exists(file.toPath().resolve("player_model.bbmodel"))) load = (byte) (load | 2);
                else if (Files.exists(file.toPath().resolve("script.lua"))) load = (byte) (load | 4);
            }

            //add to list if valid
            if (load > 0) return true;
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load avatar " + file.getName());
            e.printStackTrace();
        }

        return false;
    }

    public static void sortFolders(Map<String, LocalAvatar> toSort, Map<String, LocalAvatar> target) {
        toSort.forEach((k, v) -> {
            if (v instanceof LocalAvatarFolder)
                target.put(k, v);
        });
        toSort.forEach((k, v) -> {
            if (!(v instanceof LocalAvatarFolder))
                target.put(k, v);
        });
    }

    public static void loadResourceAvatars(ResourceManager manager) {
        //clear old entries, since we're going to re-add them, if existent
        RESOURCE_FOLDER.children.clear();

        //find all moon files inside the avatar folder
        Map<Identifier, Resource> resources = manager.findResources("avatars", identifier -> identifier.getPath().endsWith(".moon"));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier id = entry.getKey();
            Resource res = entry.getValue();

            try {
                //get name and its upper-folders
                String[] split = id.getPath().split("/");
                String name = split[split.length - 1];
                name = name.substring(0, name.length() - 5);

                //get root folder (resource pack name)
                String folder = res.getResourcePackName();
                LocalAvatar avatar = RESOURCE_FOLDER.children.get(folder);

                LocalAvatarFolder folderAvatar = null;
                if (avatar == null) {
                    folderAvatar = new LocalAvatarFolder(folder, true);
                    RESOURCE_FOLDER.children.put(folder, folderAvatar);
                } else if (avatar instanceof LocalAvatarFolder avatarFolder) {
                    folderAvatar = avatarFolder;
                }

                if (folderAvatar == null)
                    throw new Exception();

                //get parent folder
                for (int i = 1; i < split.length - 1; i++) {
                    String parent = split[i];
                    LocalAvatar parentFolder = folderAvatar.children.get(parent);

                    if (parentFolder == null) {
                        parentFolder = new LocalAvatarFolder(parent, true);
                        folderAvatar.children.put(parent, parentFolder);
                        folderAvatar = (LocalAvatarFolder) parentFolder;
                    } else if (parentFolder instanceof LocalAvatarFolder avatarFolder) {
                        folderAvatar = avatarFolder;
                    }
                }

                //add avatar
                ResourceAvatar resourceAvatar = new ResourceAvatar(name, NbtIo.readCompressed(res.getInputStream()));
                folderAvatar.children.put(name, resourceAvatar);

                FiguraMod.LOGGER.info("Loaded avatar: [" + res.getResourcePackName() + "] -> " + id.getPath().split("/", 2)[1]);
            } catch (Exception e) {
                e.printStackTrace();
                FiguraMod.LOGGER.error("Failed to load resource avatar: " + id.getPath());
            }
        }

        updateResourceExpanded(RESOURCE_FOLDER);
    }

    public static void updateResourceExpanded(LocalAvatarFolder folder) {
        //folder
        folder.expanded = !FOLDER_DATA.containsKey(folder.name) || FOLDER_DATA.get(folder.name);

        //children
        for (LocalAvatar localAvatar : folder.children.values()) {
            if (localAvatar instanceof LocalAvatarFolder child)
                updateResourceExpanded(child);
        }
    }

    public static class LocalAvatar {
        public final String name;

        public LocalAvatar(String path) {
            this.name = path;
        }
    }

    public static class LocalAvatarFolder extends LocalAvatar {
        public boolean expanded;
        public final Map<String, LocalAvatar> children = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public LocalAvatarFolder(String path, boolean expanded) {
            super(path);
            this.expanded = expanded;
        }
    }

    public static class ResourceAvatar extends LocalAvatar {
        public final NbtCompound nbt;

        public ResourceAvatar(String path, NbtCompound nbt) {
            super(path);
            this.nbt = nbt;
        }
    }
}
