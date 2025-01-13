package de.werwolf2303.xpairdebug;

import java.util.HashMap;

public class ArgParser {
    private HashMap<String, String> args;

    public ArgParser(String[] comArgs) {
        args = new HashMap<>();
        if(comArgs.length == 1) {
            return;
        }
        for(int i = 1; i < comArgs.length; i++) {
            args.put(comArgs[i].split("=")[0].replaceFirst("--", ""), comArgs[i].replace(comArgs[i].split("=")[0] + "=", ""));
        }
    }

    public String getArg(String arg) {
        return args.get(arg);
    }
}
