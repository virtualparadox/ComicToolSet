## 📚 ComicToolSet

**ComicToolSet** is a modular Java toolset for downloading, organizing, and processing comic books from online sources. The project supports multiple operations such as:

- \`download\`: Fetch issues and pages from online comic archives
- \`pack\`: Bundle downloaded issues into CBZ format
- \`translate\`: Auto-translate comic text bubbles using OCR + ML *(coming soon)*

---

## 🧰 \`download\` Command

The \`download\` command fetches all available issues and their pages from a supported online source (currently: [ReadComicOnline.li](https://readcomiconline.li)) and stores them locally as image files.

### 🔧 Usage

```bash
java -jar ComicToolSet.jar download \\
--comicRoot https://readcomiconline.li/Comic/Dylan-Dog-1986 \\
--outputFolder /Users/you/Documents/comics/dylan-dog
```

### 📄 Arguments

| Argument         | Description                                            | Required |
|------------------|--------------------------------------------------------|----------|
| \`--comicRoot\`    | The root URL of the comic on the source site           | ✅       |
| \`--outputFolder\` | Local folder where issues and pages will be downloaded | ✅       |

### 📁 Example Output Structure

```
/Users/you/Documents/comics/dylan-dog/
├── Issue-001/
│   ├── 0001.jpg
│   ├── 0002.jpg
│   └── ...
├── Issue-002/
│   └── ...
```

---

## 📦 \`pack\` Command

The \`pack\` command converts locally downloaded comic issues into CBZ archives. Each issue folder will be zipped into a `.cbz` file with the same name as the issue directory.

### 🔧 Usage

```bash
java -jar ComicToolSet.jar pack \\
--comicFolder /Users/you/Documents/comics/dylan-dog
```

### 📄 Arguments

| Argument         | Description                                                         | Required |
|------------------|---------------------------------------------------------------------|----------|
| \`--comicFolder\` | Path to the root folder containing issue folders to be packaged     | ✅       |

### 📁 Example Input Structure

```
/Users/you/Documents/comics/dylan-dog/
├── Issue-001/
│   ├── 0001.jpg
│   ├── 0002.jpg
│   └── ...
├── Issue-002/
│   └── ...
```

### 📦 Example Output Files

```
/Users/you/Documents/comics/dylan-dog/
├── Issue-001.cbz
├── Issue-002.cbz
├── Issue-001/
│   ├── 0001.jpg
│   └── ...
├── Issue-002/
│   └── ...
```

> 📝 The original issue folders are **not deleted** after packing.

---

## 🧠 Internals

- Uses **Selenium** (headless Chrome) to trigger lazy-loading of comic images
- Automatically downloads and names images in order
- CBZ files are simple ZIP archives with a `.cbz` extension
- Designed for easy extension to support additional sources and formats
