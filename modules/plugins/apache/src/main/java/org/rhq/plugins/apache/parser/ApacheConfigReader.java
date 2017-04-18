package org.rhq.plugins.apache.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApacheConfigReader {
    private static final String EMPTY_LINE = "^[\t ]*$";
    private static final Pattern emptyLinePattern = Pattern.compile(EMPTY_LINE);
    private static final Log log = LogFactory.getLog(ApacheConfigReader.class);

    public static void buildTree(String path, ApacheParser parser) {
        searchFile(path, parser);
    }

    public static bool SearchDirectiveDuplicated(String name, String value, ApacheParser parser) {
       for (ApacheDirective dir : stack) {
          if(dir.getName() == name || dir.getValuesAsString == value) {
             log.warn("Apache configuration file already conatins the directive " + name + " with the value " + value + " .");
             throw new ApacheParserException("Apache configuration file already conatins the directive " + name + " with the value " + value + " .");
	     return true;
          }
       }

       return false;
    }

    public static void searchFile(String path, ApacheParser parser) {
        File configFile = new File(path);
        if (!configFile.exists()) {
            log.warn("Apache configuration file " + path + " was not found.");
            throw new ApacheParserException("Apache configuration file " + path + " was not found.");
        }

        BufferedReader br = null;

        try {
            FileInputStream fstream = new FileInputStream(configFile);
            br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                Matcher matcher = emptyLinePattern.matcher(strLine);
                if (!matcher.matches()) {
                    ApacheDirective dir = new ApacheDirective(strLine);
                    dir.setFile(path);
                    String name = dir.getName();
                    if (!name.equals("#")) {
                        if (name.startsWith("</"))
                            parser.endNestedDirective(dir);
                        else if (name.startsWith("<"))
                            parser.startNestedDirective(dir);
                        else
                            parser.addDirective(dir);
                    }
                }
            }
            br.close();

        } catch (Exception e) {
            try {
                if (br != null)
                    br.close();
            } catch (Exception ee) {
            }
            throw new ApacheParserException(e);
        }
    }
}
