package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class DataChecker {

    private static final File output_dir = Utils.TVDB_DIR;
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		new File(output_dir, "檢核").mkdirs();
		new File(output_dir, "單位").mkdirs();
		
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("print.print_footerleft", "");
		profile.setPreference("print.print_footerright", "");
		profile.setPreference("print.print_headerleft", "");
		profile.setPreference("print.print_headerright", "");
		profile.setPreference("print_printer", "Bullzip PDF Printer");
		profile.setPreference("printer_Bullzip_PDF_Printer.print_footerleft", "");
		profile.setPreference("printer_Bullzip_PDF_Printer.print_footerright", "");
		profile.setPreference("printer_Bullzip_PDF_Printer.print_headerleft", "");
		profile.setPreference("printer_Bullzip_PDF_Printer.print_headerright", "");
		profile.setPreference("print.always_print_silent", true);
		
		WebDriver driver = new FirefoxDriver(profile);
        
		Utils.openTvdb(driver, "資料檢核");
        (new WebDriverWait(driver, 10)).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("資 料 檢 核")));
        driver.findElement(By.partialLinkText("資 料 檢 核")).click();

        // Cross-table checkers
        
        remove關閉視窗Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(1).click();
        
        waitFor關閉視窗Button(driver);
        
        downloadChecker(driver);
        
        // Other checkers
        
        remove關閉視窗Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(2).click();
        
        waitFor關閉視窗Button(driver);
        
        downloadChecker(driver);
        
        // Start to copy checker result files to unit folders
        Map<String, List<String>> tableUnits = new HashMap<String, List<String>>();
        Set<String> unitSet = new HashSet<String>();
        
		Utils.obtainTableUnitMapping(driver, tableUnits, unitSet);
		
        for(String unit: unitSet) {
    		new File(output_dir, "單位/"+unit+"/檢核疑義").mkdirs();
        }
        
        copyNoDataFilesToUnits(tableUnits);
        
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void downloadChecker(WebDriver driver) {
		// The checker's select-tag reloads every time a item is clicked, thus has to be addressed by index
        Select checkers = new Select(driver.findElement(By.name("TabName")));
        int checkerCount = checkers.getOptions().size();
        
        checkers.selectByIndex(checkerCount-1); // click the last first to ensure the page is reloaded on selecting the first
        waitFor關閉視窗Button(driver);
        remove關閉視窗Button(driver);
        
        Pattern patternNoData = Pattern.compile("該表冊無(交叉)?檢核記錄");
        
        for(int i=0; i<checkerCount; i++) {
            // remove a button and check its re-appearing as a sign of page loaded
            remove關閉視窗Button(driver);
            
            checkers = new Select(driver.findElement(By.name("TabName")));

            WebElement checker = checkers.getOptions().get(i);
        	String checkerName = checker.getText().trim().replace('/', '及');
        	checker.click();
        	
            waitFor關閉視窗Button(driver);
            
            String html = driver.findElement(By.tagName("body")).getText();
            if(!patternNoData.matcher(html).find()) {
            	// Has data
                File renamed = new File(output_dir, "檢核/檢核_"+checkerName+".pdf");
                
                if(renamed.exists())
                	continue;
                
                ((JavascriptExecutor)driver).executeScript(
                		"var imgs = document.getElementsByTagName('img'); " +
                		"var len=imgs.length; " +
                		"for(var i=len-1; i>=0; i--) {imgs[i].parentNode.removeChild(imgs[i]);} " +
                		"var inputs = document.getElementsByTagName('input'); " +
                		"var len=inputs.length; " +
                		"for(var i=len-1; i>=0; i--) {inputs[i].parentNode.removeChild(inputs[i]);} " +
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

	private static void copyNoDataFilesToUnits(Map<String, List<String>> tableUnits) {
		File[] checkers = new File(output_dir, "檢核").listFiles();
		
		for(File checker: checkers) {
	        Set<String> units = new HashSet<String>();
	        
	        // Parse checker name to figure out related tables
	        Matcher tableFinder = Pattern.compile("\\d+(_|-)\\d+((_|-)\\d+)?").matcher(checker.getName());
	        while(tableFinder.find()) {
	        	List<String> unit = tableUnits.get("table"+tableFinder.group().replace('-', '_'));
	        	if(unit == null || unit.isEmpty()) {
	        		System.err.println("No unit-in-charge: [table"+tableFinder.group()+"]");
	        	}
	        	else {
	            	units.addAll(unit);
	        	}
	        }
	        
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
