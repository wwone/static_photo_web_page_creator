Example input files used by the static HTML picture creator.

The 'text' file contains 4-line entries, one per photo/image.

The 'project.json' file contains JSON that describes the web page we are creating. That constitutes a 'project'. 
This example contains much self-documentation. It should work out of the box and give you an example 
to use to make your own photo pages.

- - - - - - 

When you have compiled the Java code, and have set up the working environment,
place these two files in that working area.

Enter:

sh runit EXAMPLE

The program will concatenate EXAMPLE with "_input.txt" to find the picture input text file.

It will concatenate EXAMPLE with "_project.json" to find the JSON project description file.

After it runs correctly, the program will create "EXAMPLE_test.html" as the static picture web page file.

This naming convention allows you to have any number of unique projects (web page setups) in the
working directory.
