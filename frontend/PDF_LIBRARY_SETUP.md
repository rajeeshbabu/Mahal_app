# PDF Library Setup Instructions

To enable PDF generation from HTML certificates, you need to add the OpenHTMLToPDF library JAR files to your project.

## Required JAR Files

Download the following JAR files and place them in the `lib` folder:

1. **openhtmltopdf-pdfbox-1.0.10.jar** (or latest version)
   - Download from: https://github.com/danfickle/openhtmltopdf/releases
   - Direct Maven Central link: https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.10/

2. **openhtmltopdf-core-1.0.10.jar** (or latest version)
   - Download from: https://github.com/danfickle/openhtmltopdf/releases
   - Direct Maven Central link: https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-core/1.0.10/

3. **slf4j-api-1.7.36.jar** (or compatible version)
   - Download from: https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/

4. **pdfbox-2.0.27.jar** (or compatible version)
   - Download from: https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/2.0.27/

5. **fontbox-2.0.27.jar** (or compatible version)
   - Download from: https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/2.0.27/

6. **commons-logging-1.2.jar** (or compatible version)
   - Download from: https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/

7. **xmpbox-2.0.27.jar** (or compatible version)
   - Download from: https://repo1.maven.org/maven2/org/apache/pdfbox/xmpbox/2.0.27/

## Quick Download Script (Windows PowerShell)

You can use this PowerShell script to download all required JARs:

```powershell
$libDir = "lib"
if (-not (Test-Path $libDir)) { New-Item -ItemType Directory -Path $libDir }

$jars = @(
    "https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.10/openhtmltopdf-pdfbox-1.0.10.jar",
    "https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-core/1.0.10/openhtmltopdf-core-1.0.10.jar",
    "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
    "https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/2.0.27/pdfbox-2.0.27.jar",
    "https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/2.0.27/fontbox-2.0.27.jar",
    "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar",
    "https://repo1.maven.org/maven2/org/apache/pdfbox/xmpbox/2.0.27/xmpbox-2.0.27.jar"
)

foreach ($url in $jars) {
    $fileName = $url.Split('/')[-1]
    $filePath = Join-Path $libDir $fileName
    Write-Host "Downloading $fileName..."
    Invoke-WebRequest -Uri $url -OutFile $filePath
    Write-Host "Downloaded: $filePath"
}

Write-Host "`nAll JAR files downloaded successfully!"
```

## Manual Setup Steps

1. Create the `lib` folder if it doesn't exist
2. Download each JAR file from the Maven Central links above
3. Place all JAR files in the `lib` folder
4. Make sure your IDE/build system includes the `lib` folder in the classpath
5. Rebuild your project

## Verification

After adding the JARs, rebuild your project. The PDF generation should work automatically when you create or regenerate certificates.

## Alternative: Using Maven or Gradle

If you prefer to use a build tool, add these dependencies:

**Maven (pom.xml):**
```xml
<dependencies>
    <dependency>
        <groupId>com.openhtmltopdf</groupId>
        <artifactId>openhtmltopdf-pdfbox</artifactId>
        <version>1.0.10</version>
    </dependency>
</dependencies>
```

**Gradle (build.gradle):**
```gradle
dependencies {
    implementation 'com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10'
}
```

