



				    !!!!!HOW TO RUN THE JAVA SIDE!!!!!



	1. Make sure jmdns-3.4.1.jar is downloaded and in the lib folder.

	
	2. Make sure it is in the build path, adding as an external jar if not.


	3. Make sure you are in Java directory and run the following:


			javac -classpath "lib/jmdns-3.4.1.jar:src/main" src/main/*.java


	4. Then, run: 

			cd src

	5. Finally, run:

			java -cp .:../lib/jmdns-3.4.1.jar main.MainApp