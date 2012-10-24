package tvdb;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class BasicDataDownloader {

    private static final File output_dir = new File("D:/work/tvdb/download");
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		new File(output_dir, "基本資料表").mkdirs();
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
		
		Utils.openTvdb(driver, "基本表冊");
		
        driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("基本資料庫報表")).click();
        
        (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        downloadTables(driver, "基本資料表");
        
        // Classify tables to organization units
        driver.navigate().back();
        driver.navigate().back();
        (new WebDriverWait(driver, 10)).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("輸入表冊一覽表")));
        driver.findElement(By.partialLinkText("輸入表冊一覽表")).click();
        (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        

        // Read tables filler table and construct the mapping
        SortedMap<String, List<String>> tableUnits = new TreeMap<String, List<String>>();
        Set<String> unitSet = new HashSet<String>();
        
        List<WebElement> trs = driver.findElement(By.tagName("table")).findElements(By.tagName("tr"));
        trs.remove(0); // remove table header
        
        for(WebElement tr: trs) {
        	
        	List<WebElement> tds = tr.findElements(By.tagName("td"));
        	String tableName = tds.get(1).getText();
        	String unitStr = tds.get(5).getText();
        	
        	List<String> units;
        	
        	if(!tableUnits.containsKey(tableName)) {
        		tableUnits.put(tableName, new ArrayList<String>());
        	}
        	
        	units = tableUnits.get(tableName);
        	
        	for(String unit: unitStr.split(",")) {
        		unit = unit.trim();
        		if(unit.isEmpty())
        			continue;
        		
        		units.add(unit);
        	}
        	
        	unitSet.addAll(units);
        }
        
        for(String unit: unitSet) {
    		new File(output_dir, "單位/"+unit).mkdir();
        }
        
        // Copy table files to the unit in charge according to the table-unit mapping
        for(String tableName: tableUnits.keySet()) {
        	if(tableUnits.get(tableName).isEmpty()) {
        		System.err.println("["+tableName+"]: no unit in charge");
        		continue;
        	}
        	
        	File tableFile = new File(output_dir, "基本資料表/"+tableName+".pdf");
        	
        	for(String unit: tableUnits.get(tableName)) {
        		File unitDir = new File(output_dir, "單位/"+unit);
        		
            	if(!tableFile.exists()) {
            		try {
						new File(unitDir, tableName+"-無資料.txt").createNewFile();
	            		System.out.println("["+tableName+"-無資料] => ["+unitDir.getName()+"]");
					} catch (IOException e) {
						System.err.println("Error creating empty file for ["+tableName+"]");
					}
            		continue;
            	}
            	
        		try {
        			File targetFile = new File(unitDir, tableFile.getName());
        			
        			if(!targetFile.exists()) {
    					FileUtils.copyFile(tableFile, targetFile);
        			}
					System.out.println("["+tableName+"] => ["+unitDir.getName()+"]");
				} catch (IOException e) {
					System.err.println("["+tableName+"] => ["+unitDir.getName()+"]: failed");
				}
        	}
        }
     
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void downloadTables(WebDriver driver, String folder) throws InterruptedException {
		// Begin download
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        File finalDir;
        
        if(folder == null) {
        	finalDir = output_dir;
        }
        else {
            finalDir = new File(output_dir, folder);
        }
        
        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.tagName("a"));
        for (WebElement link : tables) {
        	if(!link.getText().matches("^report\\d+(_|-)\\d+((_|-)\\d+)?$") && !link.getText().matches("^table\\d+(_|-)\\d+((_|-)\\d+)?$"))
        		continue;
        	
        	String finalName = link.getText()+".pdf";
			
            File finalFile = new File(finalDir, finalName);
            
        	if(finalFile.exists())
        		continue;
        	
        	link.click();
            for(String hWnd : driver.getWindowHandles()) {
            	if(!hWnd.equals(mainWin)) {
            		newWin = hWnd;
            		break;
            	}
            }
            
            driver.switchTo().window(newWin);
            (new WebDriverWait(driver, 60)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
//            ((JavascriptExecutor)driver).executeScript("var fileref=document.createElement('script'); fileref.setAttribute('type','text/javascript');" +
//            		"fileref.setAttribute('src', 'http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js1')");
//            ((JavascriptExecutor)driver).executeScript("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js\"></script>");
            //((JavascriptExecutor)driver).executeScript("$(function() {$('img').hide();});");
            ((JavascriptExecutor)driver).executeScript(
            		"var imgs = document.getElementsByTagName('img'); " +
            		"var len=imgs.length; " +
            		"for(var i=len-1; i>=0; i--) {imgs[i].parentNode.removeChild(imgs[i]);} " +
            		"window.print();");
            
            File newFile = null;
            
            while(true) {
            	File[] files = Utils.BULLZIP_DIR.listFiles(new FileFilter() {
					public boolean accept(File f) {
						if(!f.isFile())
							return false;
						else if(f.length() == 0)
							return false;
						else
							return true;
					}
            	});
            	
            	if(files.length == 0) {
            		Thread.sleep(300);
            		continue;
            	}
            	
            	if(files.length > 1) {
            		System.err.println("More than one output file");
            	}
            	
            	newFile = files[0];
            	break;
            }
            
            if(finalFile.exists()) {
            	finalFile.delete();
            }
            while(!newFile.renameTo(finalFile)) {
            	System.out.println("Failed to rename "+newFile.getName()+" to "+finalFile.getPath());
            	Thread.sleep(1000);
            }
            driver.close();
            driver.switchTo().window(mainWin);
//            break;
		}
	}

}
