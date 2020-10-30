package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class DataChecker {

    private static final File output_dir = Utils.TVDB_DIR;
    private static final String unforgivablePrefix = "{不得檢出} ";
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		new File(output_dir, "檢核").mkdirs();
		new File(output_dir, "單位").mkdirs();

		downloadCheckers();
		
        copyCheckResultToUnits();
        
        System.err.println("有部份交叉檢核表不會在名稱上標明所有相關表冊，請逐檔檢查。");
	}

	private static void downloadCheckers() throws InterruptedException, IOException {
		WebDriver driver = Utils.createFireFoxDriver();
        
		Utils.openTvdb(driver, "資料檢核");
        (new WebDriverWait(driver, 10)).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("資 料 檢 核")));
        driver.findElement(By.partialLinkText("資 料 檢 核")).click();

        // Cross-table checkers
        
        remove關閉視窗Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(1).click();
        
        waitFor關閉視窗Button(driver);
        
        downloadCurrentGroupOfCheckers(driver, "交叉");
        
        // Other checkers
        
        remove關閉視窗Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(2).click();
        
        waitFor關閉視窗Button(driver);
        
        downloadCurrentGroupOfCheckers(driver, "其他");
        
        // Statistic checkers
        
        remove關閉視窗Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(3).click();
        
        waitFor關閉視窗Button(driver);
        
        downloadCurrentGroupOfCheckers(driver, "統計處");
		((JavascriptExecutor)driver).executeScript("alert('Done! 有部份交叉檢核表不會在名稱上標明所有相關表冊，請逐檔檢查。')");
	}
	
	private static void downloadCurrentGroupOfCheckers(WebDriver driver, String groupName) throws IOException {
		Set<String> unforgivable = Utils.obtain不得檢出之檢核表();
	    
	    // The checker's select-tag reloads every time a item is clicked, thus has to be addressed by index
        Select checkers = new Select(driver.findElement(By.name("TabName")));
        int checkerCount = checkers.getOptions().size();
        
        checkers.selectByIndex(checkerCount-1); // click the last first to ensure the page is reloaded on selecting the first
        waitFor關閉視窗Button(driver);
        remove關閉視窗Button(driver);
        
        Pattern patternNoData1 = Pattern.compile("該表冊無(交叉)?檢核記錄");
        Pattern patternNoData2 = Pattern.compile("無檢核資料");
        
        for(int i=0; i<checkerCount; i++) {
            // remove a button and check its re-appearing as a sign of page loaded
            remove關閉視窗Button(driver);
            
            checkers = new Select(driver.findElement(By.name("TabName")));

            WebElement checker = checkers.getOptions().get(i);
            /* used to using option name to figure out relevent tables; now use 相關表冊 info instead */
        	String optionName = checker.getText().trim();
        	checker.click();
        	
			try {
				waitFor關閉視窗Button(driver);
			} catch(Exception e) {
				System.err.println("Error waiting for "+optionName+"; skipped");
			}
            
            String checkerName = Utils.normalizeCheckerName(optionName);
            boolean isForgivable = unforgivable.contains(checkerName);
            
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
            List<WebElement> related = driver.findElements(By.xpath("//td[text()='相關表冊']/following-sibling::td"));
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

            if(!related.isEmpty()) {
                checkerName = checkerName+"["+related.get(0).getText().trim()+"]";
            }
           
            String html = driver.findElement(By.tagName("body")).getText();
            if(!patternNoData1.matcher(html).find() && !patternNoData2.matcher(html).find()) {
            	// Has data
                File renamed = new File(output_dir, "檢核/"+(isForgivable? unforgivablePrefix : "")+groupName+"-"+checkerName+".pdf");
                
                if(renamed.exists())
                	continue;
                
                ((JavascriptExecutor)driver).executeScript(
                		"var imgs = document.getElementsByTagName('img'); " +
                		"var len=imgs.length; " +
                		"for(var i=len-1; i>=0; i--) {imgs[i].parentNode.removeChild(imgs[i]);} " +
                		"window.print();");
                
                File output = Utils.waitForGeneratedFile(Utils.BULLZIP_DIR);
                if(!output.renameTo(renamed)) {
                	System.err.println("Rename failed: ["+renamed.getName()+"]");
                	output.renameTo(new File(output_dir, "檢核/"+output.getName()));
                	continue;
                }
             
            }
        }
	}

	private static void copyCheckResultToUnits() throws IOException {
        Map<String, List<String>> tableUnits = new HashMap<String, List<String>>();
        Set<String> unitSet = new HashSet<String>();
        
		Utils.obtainTableUnitMapping(tableUnits, unitSet);
		Utils.obtain非當期TableUnitMapping(tableUnits, unitSet);
		
        for(String unit: unitSet) {
    		new File(output_dir, "單位/"+unit+"/檢核疑義").mkdirs();
        }
		
		File[] checkers = new File(output_dir, "檢核").listFiles();
		
		Pattern patRelated = Pattern.compile(".*\\[([^]]*)\\]\\.[a-z]+$");
		for(File checker: checkers) {
	        Set<String> units = new HashSet<String>();
	        String relatedTables;
	        Matcher matcher = patRelated.matcher(checker.getName());
	        if(matcher.matches()) {
	            relatedTables = matcher.group(1);
	        }
	        else {
	            relatedTables = checker.getName();
	            if(relatedTables.startsWith("統計處-報表") || relatedTables.startsWith(unforgivablePrefix+"統計處-報表")) {
					// lest the leading "報表" confuse the resolution of related tables
	                relatedTables = relatedTables.substring(6);
	            }
	        }
	        
	        Utils.obtainUnitsOfChecker(relatedTables, tableUnits, units);
	        
	        if(units.isEmpty()) {
	        	System.err.println("No unit-in-charge: ["+checker.getName()+"]");
	        	continue;
	        }
	        
	        for(String unit: units) {
	        	try {
					FileUtils.copyFileToDirectory(checker, new File(output_dir, "單位/"+unit+"/檢核疑義"));
					System.out.println("["+checker.getName()+"] => ["+unit+"]");
				} catch (IOException e) {
					System.err.println("Error: ["+checker.getName()+"] => ["+unit+"]");
				}
	        }
		}
	}

	private static void waitFor關閉視窗Button(WebDriver driver) {
		(new WebDriverWait(driver, 10)).until(
        		ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='button'][value='關閉視窗']")));
	}

	private static void remove關閉視窗Button(WebDriver driver) {
		((JavascriptExecutor)driver).executeScript(
				"var inputs = document.getElementsByTagName('input');" +
				"for(var i=inputs.length-1; i>=0; i--) {" +
				"  if(inputs[i].type=='button' && inputs[i].value=='關閉視窗') {" +
				"    inputs[i].parentNode.removeChild(inputs[i]);" +
				"  }" +
				"}");
	}

}
