# Malayalam Font Setup Instructions

To display Malayalam (and other Unicode) text in PDF certificates, you need to add a Unicode-supporting font to your project.

## Quick Setup

1. **Download a Malayalam font:**
   - Go to: https://fonts.google.com/noto/specimen/Noto+Sans+Malayalam
   - Click "Download family" to get the font files
   - Extract the ZIP file

2. **Add font to project:**
   - Create a `fonts` folder in your project root (same level as `src`, `lib`, etc.)
   - Copy the file `NotoSansMalayalam-Regular.ttf` to the `fonts` folder
   - Your project structure should look like:
     ```
     JavaApp/
     ├── fonts/
     │   └── NotoSansMalayalam-Regular.ttf
     ├── src/
     ├── lib/
     └── ...
     ```

3. **Restart the application:**
   - The application will automatically detect and use the font from the `fonts` folder

## Alternative Fonts

You can also use any of these Malayalam-supporting fonts:
- **Noto Sans Malayalam** (Recommended) - https://fonts.google.com/noto/specimen/Noto+Sans+Malayalam
- **Rachana** - https://www.google.com/get/noto/
- **Meera** - https://www.google.com/get/noto/
- **Lohit Malayalam** - Available in Linux distributions

## Verification

After adding the font, when you generate a PDF with Malayalam content, you should see in the console:
```
Successfully loaded font from fonts directory: NotoSansMalayalam-Regular.ttf
```

If you see a warning instead, check:
- The `fonts` folder exists in the project root
- The font file has a `.ttf` or `.otf` extension
- The font file is not corrupted

## Notes

- The font file will be embedded in the PDF, so the PDF will work on any system
- You only need the Regular (non-bold) version for most text
- If you need bold text, you can also add `NotoSansMalayalam-Bold.ttf`

