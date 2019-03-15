package tvdb;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class MainMenu {

    public static void main(String[] args) throws IOException {
        Reflections reflections = new Reflections(MainMenu.class.getPackage().getName(), new SubTypesScanner(false));

        List<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class).stream()
            .filter(cls -> hasMainMethod(cls))
            .collect(toList());

        IntStream.range(0, allClasses.size())
                .forEach(idx -> System.out.println(String.format("(%d) %s", idx, allClasses.get(idx).getName())));

        int choice = getUserChoice(0, allClasses.size()-1);
        executeMainMethod(allClasses.get(choice));
    }
    
    private static int getUserChoice(int min, int max) throws IOException {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new CloseShieldInputStream(System.in)))) {
            while(true) {
                String line = br.readLine();
                try {
                    int choice = Integer.parseInt(line);
                    if(choice < min || choice > max) {
                        System.err.println("Illegal choice "+choice+"; valid range: "+min+"~"+max);
                        continue;
                    }
                    else {
                        return choice;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Please enter a number.");
                }
            }
        }
    }

    private static void executeMainMethod(Class<?> cls) {
        try {
            Method mainMethod = cls.getDeclaredMethod("main", String[].class);
            mainMethod.invoke(null, (Object)new String[] {});
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static boolean hasMainMethod(Class<?> cls) {
        try {
            return cls.getDeclaredMethod("main", String[].class) != null;
        } catch (Exception e) {
            return false;
        }
    }

}
