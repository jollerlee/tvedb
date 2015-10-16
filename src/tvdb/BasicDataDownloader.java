package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
import org.openqa.selenium.support.ui.WebDriverWait;


public class BasicDataDownloader {

    private static final File output_dir = Utils.TVDB_DIR;
    private static final File download_dir = new File(output_dir, "temp");
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		new File(output_dir, "單位").mkdirs();
		download_dir.mkdirs();
		
		FirefoxProfile profile = Utils.createFireFoxProfile();
		profile.setPreference("browser.download.dir", download_dir.getPath());

		WebDriver driver = new FirefoxDriver(profile);
		
		Utils.openTvdb(driver, "基本表冊");
		
        driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("基本資料庫報表")).click();
        
        (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        downloadTables(driver, "基本資料表", EnumSet.allOf(OutputType.class));
        
        SortedMap<String, List<String>> tableUnits = new TreeMap<String, List<String>>();
        Set<String> unitSet = new HashSet<String>();
        
        Utils.obtainTableUnitMapping(driver, tableUnits, unitSet);
        
        for(String unit: unitSet) {
    		new File(output_dir, "單位/"+unit+"/表冊資料").mkdirs();
    		new File(output_dir, "單位/"+unit+"/無資料表冊").mkdirs();
        }
        
        // Copy table files to the unit in charge according to the table-unit mapping
        for(String tableName: tableUnits.keySet()) {
        	if(tableUnits.get(tableName).isEmpty()) {
        		System.err.println("["+tableName+"]: no unit in charge");
        		continue;
        	}
        	
        	for(OutputType type: EnumSet.allOf(OutputType.class)) {
            	File tableFile = new File(output_dir, "基本資料表/"+type.name+"/"+tableName+type.ext);
            	boolean tableFileExists = tableFile.exists();
            	if(!tableFileExists) {
        			File noData = new File(output_dir, "基本資料表/"+tableName+"-無資料.txt");
        			if(!noData.exists()) {
	            		try {
            				noData.createNewFile();
            			}
	            		catch(IOException e) {
							System.err.println("Error creating no-data file for ["+tableName+"]");
	            		}
            		}
            	}
            	
            	for(String unit: tableUnits.get(tableName)) {
            		File unitDir = new File(output_dir, "單位/"+unit);
            		
            		// handle no-data tables
                	if(!tableFileExists) {
						File noData = new File(unitDir, "無資料表冊/"+tableName+".txt");
	            		System.out.println("["+tableName+"-無資料] => ["+unit+"]");
	            		
						if(!noData.exists()) {
							try {
    							noData.createNewFile();
	    					} catch (IOException e) {
	    						System.err.println("No-data: Error creating file for ["+tableName+"] ("+unit+")");
	    					}
						}
                		continue;
                	}
                	
                	// Copy basic data tables
            		try {
            			File targetFile = new File(unitDir, "表冊資料/"+tableFile.getName());
            			
            			if(!targetFile.exists()) {
        					FileUtils.copyFile(tableFile, targetFile);
            			}
    					System.out.println("["+tableFile.getName()+"] => ["+unit+"]");
    				} catch (IOException e) {
    					System.err.println("Error: ["+tableFile.getName()+"] => ["+unit+"]");
    				}
            	}
        	}
        }
     
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void downloadTables(WebDriver driver, String folder, EnumSet<OutputType> types) throws InterruptedException {
		// Begin download
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        
		for(OutputType type: types) {
			new File(output_dir, folder+"/"+type.name).mkdirs();
		}
		
    	Pattern patReport = Pattern.compile("^(report\\d+(_|-)\\d+((_|-)\\d+)?).*$");
    	Pattern patTable = Pattern.compile(".*(table\\d+(_|-)\\d+((_|-)\\d+)?).*$");

        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.tagName("a"));
        for (WebElement link : tables) {
        	String linkText = link.getText();
        	Matcher matcher;
        	if((matcher = patReport.matcher(linkText)).matches() ||
        			(matcher = patTable.matcher(linkText)).matches()) {
        		linkText = matcher.group(1);
        	}
        	else {
        		System.out.println("Ignored link: "+linkText);
        		continue;
        	}
        	
        	boolean pageLoaded = false;
        	
        	for(OutputType type: types) {
            	String finalName = linkText.replace('_', '-')+type.ext;
            	finalName = finalName.replaceFirst(".*table", "");

            	File finalOutput = new File(output_dir, folder+"/"+type.name+"/"+finalName);
    			
            	if(finalOutput.exists())
            		continue;
            	
            	if(!pageLoaded) {
                	link.click();
                    for(String hWnd : driver.getWindowHandles()) {
                    	if(!hWnd.equals(mainWin)) {
                    		newWin = hWnd;
                    		break;
                    	}
                    }
                    driver.switchTo().window(newWin);
                    (new WebDriverWait(driver, 60)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
                    ((JavascriptExecutor)driver).executeScript(
                    		"var imgs = document.getElementsByTagName('img'); " +
                    		"var len=imgs.length; " +
                    		"for(var i=len-1; i>=0; i--) {imgs[i].parentNode.removeChild(imgs[i]);} ");
                    
                    pageLoaded = true;
            	}
            	
            	type.download(driver, finalOutput);
                
        	}
        	if(pageLoaded) {
                driver.close();
                driver.switchTo().window(mainWin);
        	}
//            break;
		}
	}

    private static enum OutputType { 
    	EXCEL("Excel", ".xls", (driver) -> {
//			driver.findElement(By.partialLinkText("匯出Excel檔")).click();
			// With the above method, sometimes it hangs waiting for the response after clicking, while the file has already been saved
			((JavascriptExecutor)driver).executeScript(driver.findElement(By.partialLinkText("匯出Excel檔")).getAttribute("href"));
    	}, download_dir),
    	
    	PDF("PDF", ".pdf", (driver) -> {
			((JavascriptExecutor)driver).executeScript("window.print();");
    	}, Utils.BULLZIP_DIR);
    	
    	interface TypeDownloader {
    		void download(WebDriver driver);
    	}
    	
    	public final String name;
    	public final String ext;
		private TypeDownloader downloader;
		private File generatedFileFolder;
    	
    	OutputType(String name, String ext, TypeDownloader downloader, File genFileFolder) {
    		this.name = name;
    		this.ext = ext;
    		this.downloader = downloader;
    		this.generatedFileFolder = genFileFolder;
    		
    		generatedFileFolder.mkdirs();
    		
	        for(File f: generatedFileFolder.listFiles()) {
	        	if(f.isFile()) {
	        		f.delete();
	        	}
	        }
    	}
    	
    	void download(WebDriver driver, File output) {
    		downloader.download(driver);
    		File newFile = Utils.waitForGeneratedFile(generatedFileFolder);
    		
            if(!newFile.renameTo(output)) {
            	System.out.println("Failed to rename ["+newFile.getName()+"] to "+output.getPath());
            	newFile.renameTo(new File(output.getParentFile(), newFile.getName()));
            }
    	}
    };
}
