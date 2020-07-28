# static_photo_web_page_creator
This static HTML file creator is an upgrade to the Perl code that I have here on GitHub. 

This creator is written in Java and uses a more limited set of variable input files. Boilerplate web HTML is kept in a single file that is only altered if you want to change the look and feel of the pages. Each 'project' requires a text input file with the names, captions, and locations of the photos, and a JSON 'project' file that describes the metadata needed to create the non-photo parts of the web page.

This code creates more up-to-date web pages, invoking a more recent version of the Vanilla layout. 

The 'boilerplate' for the web pages is kept in a JSON file, allowing for more flexibility in design, and use of alternative designs. It should be easy to have different pages created by different boilerplate input, while the 'project'-specific information (images and text) do not change.

For the purposes of showcasing photos, it invokes a Vanilla dark-themed background.

The example directory contains input files that will make an example static photo web page.

