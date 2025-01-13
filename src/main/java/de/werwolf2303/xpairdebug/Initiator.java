package de.werwolf2303.xpairdebug;

import java.io.IOException;

public class Initiator {
    public static void main(String[] args) throws IOException {
        if(args.length == 0) {
            printHelp();
            return;
        }

        ArgParser parser = new ArgParser(args);

        if(args[0].equals("server")) {
            new Broadcaster(
                    parser.getArg("cwd"),
                    parser.getArg("tmpDir")
            ).broadcast();
        } else if(args[0].equals("client")) {
            new Receiver(
                    parser.getArg("jvmArgs"),
                    parser.getArg("progArgs"),
                    parser.getArg("filePath")
            ).receive();
        } else {
            printHelp();
        }
    }

    static void printHelp() {
        System.out.println("XPAirDebug.jar <server|client>");
        System.out.println("XPAirDebug.jar client ARGS");
        System.out.println("--jvmArgs=ARGS");
        System.out.println("--progArgs=ARGS");
        System.out.println("--filePath=PATH (Required)");
        System.out.println("XPAirDebug.jar server");
        System.out.println("--cwd=PATH");
        System.out.println("--tmpDir=PATH");
    }
}
