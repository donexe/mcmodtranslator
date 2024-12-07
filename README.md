# Minecraft Mod Translator

A Java desktop application that helps translate Minecraft mod language files into multiple languages using Google Translate.

## Features

- Supports translation to over 100 languages
- Preserves Minecraft formatting codes (§-codes)
- Case-insensitive language file detection
- Drag & drop support for .jar files
- Progress tracking with ability to stop translation
- Clean and intuitive user interface

## Requirements

- Java 8 or higher
- Maven (for building from source)

## Building from Source

1. Clone the repository
2. Navigate to the project directory
3. Run: `mvn clean package`
4. The executable JAR will be in the `target` folder

## Usage

1. Launch the application
2. Select a Minecraft mod (.jar file)
3. Choose output directory (default is "translated_mods")
4. Select target language
5. Click "Translate" to start the translation process

## Technical Details

- Uses Google Translate's public API
- 50ms delay between translation requests
- Automatic language code conversion to Minecraft format
- Preserves original mod file structure

## Dependencies

- OkHttp (4.9.1) - HTTP client
- Gson (2.8.9) - JSON parsing
- Maven - Project management

## License

This project is open source and available under the MIT License.

## Contributing

Feel free to submit issues and pull requests to help improve the application.
