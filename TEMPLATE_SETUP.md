# Certificate Template Setup

The application now generates PDF certificates directly from an image template instead of HTML.

## Template Image Setup

1. **Create the templates directory** (if it doesn't exist):
   - The application will automatically create a `templates` folder in the project root

2. **Place your certificate template image**:
   - File name: `marriage_certificate_template.png` (or `.jpg`)
   - Location: `templates/marriage_certificate_template.png`
   - Format: PNG or JPG
   - Recommended size: A4 dimensions (210mm x 297mm) or similar aspect ratio

3. **Template Requirements**:
   - The template should be a blank certificate form with placeholders
   - The image will be scaled to fit A4 page size
   - Text will be overlaid on top of the template at specific positions

## Text Positioning

The application overlays text at the following approximate positions (you may need to adjust coordinates in the code):

- **Groom Name**: After "between" text
- **Groom Parent**: After "son of" text  
- **Groom Address**: Below groom parent
- **Bride Name**: After "and" text
- **Bride Parent**: After "daughter of" text
- **Bride Address**: Below bride parent
- **Date of Nikah**: After "Date of Nikah" label
- **Place of Nikah**: After "Place of Nikah" label
- **Certificate Number**: After "Certificate No." label
- **Date of Issue**: After "Date of Issue" label

## Adjusting Text Positions

If the text doesn't align correctly with your template, edit the coordinates in `CertificatePDFService.java`:

- `leftMargin`: Starting X position (default: 80)
- `startY`: Starting Y position from top (default: PAGE_HEIGHT - 150)
- `lineHeight`: Vertical spacing between lines (default: 18)
- Individual field offsets can be adjusted in the `generatePDFFromImageTemplate` method

## Example Template

Your template image should look similar to the certificate shown in the application, with blank spaces where the data will be filled in.

## Testing

After placing the template:
1. Rebuild your project
2. Create or regenerate a marriage certificate
3. The PDF will be generated with the template as background and data overlaid

