## 📚 ComicToolSet

**ComicToolSet** is a modular Java toolset for downloading, organizing, and translating comic books from online sources. The project supports multiple operations such as:

- `download`: Fetch issues and pages from online comic archives
- `pack`: Bundle issues into formats like CBZ (coming soon)
- `translate`: Auto-translate comic text bubbles using OCR + ML (coming soon)

---

## 🧰 `download` Command

The `download` command fetches all available issues and their pages from a supported online source (currently: [ReadComicOnline.li](https://readcomiconline.li)) and stores them locally as image files.

### 🔧 Usage

```bash
java -jar ComicToolSet.jar download \
  --comicRoot https://readcomiconline.li/Comic/Dylan-Dog-1986 \
  --outputFolder /Users/you/Documents/comics/dylan-dog
```

### 📄 Arguments

| Argument         | Description                                            | Required |
|------------------|--------------------------------------------------------|----------|
| `--comicRoot`    | The root URL of the comic on the source site           | ✅       |
| `--outputFolder` | Local folder where issues and pages will be downloaded | ✅       |

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

## 🧠 Internals

- Uses **Selenium** (headless Chrome) to trigger lazy-loading of comic images
- Downloads and detects image format automatically
- Fully supports modular extension for other sources or formats  
