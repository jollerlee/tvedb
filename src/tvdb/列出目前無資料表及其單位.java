package tvdb;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class 列出目前無資料表及其單位 {

    /**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		FirefoxProfile profile = Utils.createFireFoxProfile();

		WebDriver driver = new FirefoxDriver(profile);
		
		Utils.openTvdb(driver, null);
		
        driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("基本資料庫報表")).click();
        
        (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        Set<String> tablesWithData = getAllLinks(driver);
        
        SortedMap<String, List<String>> tableUnits = new TreeMap<>();
        Set<String> unitSet = new HashSet<String>();
        Utils.obtainTableUnitMapping(tableUnits, unitSet);
        
        tableUnits.keySet().stream()
                .filter(x -> !tablesWithData.contains(x))
                .forEach(x -> {
                    System.err.println("["+x+"]: " + tableUnits.get(x).stream().collect(joining(",")));
                });
        
	}

    private static Set<String> getAllLinks(WebDriver driver) {
        
        // The map to record found table links and the counts.
        Set<String> foundTables = new HashSet<>();

        Pattern pat = Pattern.compile(".*(table\\d+(_|-)\\d+((_|-)\\d+)?).*$");

        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.tagName("a"));
        for (WebElement link : tables) {
            String linkText = link.getText();
            Matcher matcher;
            if ((matcher = pat.matcher(linkText)).matches()) {
                linkText = matcher.group(1);
            } else {
                System.out.println("Ignored link: " + linkText);
                continue;
            }

            String finalName = linkText.replace('_', '-').replaceFirst(".*table", "");

            foundTables.add(finalName);
        }
        
        return foundTables;
    }

}
