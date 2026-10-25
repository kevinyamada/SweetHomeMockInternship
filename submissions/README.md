# Acceptance evidence

The PNG files are renders of the running Swing components captured by
`tools/VerifyFeatures.java` in a Linux virtual display. They are not screenshots
of Kevin's Windows desktop. Task 1 also asks you to launch the application on
your own machine; run `mvn compile exec:java` and replace its image with a local
screenshot if required by your instructor.

The PDF is produced by the application's `HomePDFPrinter` using
`samples/MultiLevelHome.sh3d`. It contains the furniture table followed by the
three level plans. The original sample file is unchanged.

Verification includes description persistence, catalog and resized furniture
volume, ascending/descending order, menu presence, page counts, hidden and
empty levels, state restoration after printing and after a rendering error.

After `mvn compile`, run the optional integration harness on a graphical desktop:

```sh
javac -cp 'target/classes:SweetHome3D/lib/*' -d target/verification tools/VerifyFeatures.java
java -cp 'target/verification:target/classes:SweetHome3D/lib/*' VerifyFeatures
```

On Windows use `;` instead of `:` in the classpath and double quotes.
The harness regenerates these evidence files. The course acceptance tests in
`assignment-tests` have not been edited.
