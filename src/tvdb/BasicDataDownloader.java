package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
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

    private static final File output_dir = Utils.TVDB_DIR;
    private static final File download_dir = new File(output_dir, "temp");
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
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
		
		profile.setPreference("browser.download.manager.showWhenStarting", false);
		profile.setPreference("browser.download.folderList", 2); // to make the following setting take effect
		profile.setPreference("browser.download.dir", download_dir.getPath());
		profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/vnd.ms-excel,application/vnd.ms-execl");
		
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
    		new File(output_dir, "單位/"+unit).mkdir();
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
            		
                	if(!tableFileExists) {
						File noData = new File(unitDir, tableName+"-無資料.txt");
	            		System.out.println("["+tableName+"-無資料] => ["+unit+"]");
	            		
						if(!noData.exists()) {
							try {
    							noData.createNewFile();
	    					} catch (IOException e) {
	    						System.err.println("Error creating no-data file for ["+tableName+"] ("+unit+")");
	    					}
						}
                		continue;
                	}
                	
            		try {
            			File targetFile = new File(unitDir, tableFile.getName());
            			
            			if(!targetFile.exists()) {
        					FileUtils.copyFile(tableFile, targetFile);
            			}
    					System.out.println("["+tableFile.getName()+"] => ["+unitDir.getName()+"]");
    				} catch (IOException e) {
    					System.err.println("["+tableFile.getName()+"] => ["+unitDir.getName()+"]: failed");
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
		
        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.tagName("a"));
        for (WebElement link : tables) {
        	String linkText = link.getText();
        	if(!linkText.matches("^report\\d+(_|-)\\d+((_|-)\\d+)?$") && !linkText.matches("^table\\d+(_|-)\\d+((_|-)\\d+)?$"))
        		continue;
        	
        	boolean pageLoaded = false;
        	
        	for(OutputType type: types) {
            	String finalName = linkText.replace('/', '及')+type.ext;
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
    	EXCEL("Excel", ".xls", new TypeDownloader() {
    		
			@Override
    		public void download(WebDriver driver) {
//    			driver.findElement(By.partialLinkText("匯出Excel檔")).click();
				// With the above method, sometimes it hangs waiting for the response after clicking, while the file has already been saved
    			((JavascriptExecutor)driver).executeScript(driver.findElement(By.partialLinkText("匯出Excel檔")).getAttribute("href"));
    		}
    	}, download_dir),
    	
    	PDF("PDF", ".pdf", new TypeDownloader() {

			@Override
			public void download(WebDriver driver) {
				((JavascriptExecutor)driver).executeScript("window.print();");
			}
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
