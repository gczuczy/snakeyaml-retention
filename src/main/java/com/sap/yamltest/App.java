package com.sap.yamltest;

import java.io.IOException;

import com.sap.yamltest.YAML;

/**
 * Hello world!
 *
 */
public class App {
  public static void main( String[] args ) throws IOException {
    String input = """
# first image
image1:
  repository: &repo some.re.po
  tag: &tag 1.2.3
# second image
image2:
  repository: *repo
  tag: *tag
some: thing
other: thing
      """;
    System.out.println("Input:\n" + input + "\n---\n");

    YAML y = new YAML(input);
    /*
    String output = yaml.dump(docnode.getValue());
    */

    try {
      y.setValue("image1.repository", "some.other.repo");
    }
    catch (Exception e) {
      System.out.println("setValue failed: " + e);
    }
    String output = y.dump();
    System.out.println("Output:\n" + output + "\n---\n");
  }
}
