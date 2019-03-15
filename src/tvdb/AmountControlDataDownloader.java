package tvdb;
import java.awt.AWTException;
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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class AmountControlDataDownloader {

    private static final File output_dir = Utils.TVDB_DIR;
    private static final File download_dir = new File(output_dir, "temp");
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws AWTException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		new File(output_dir, "單位").mkdirs();
		
		FirefoxProfile profile = Utils.createFireFoxProfile();
		profile.setPreference("browser.download.dir", download_dir.getPath());
		
		WebDriver driver = Utils.createFireFoxDriver(profile);
		
		Utils.openTvdb(driver, "總量管制報表");
    	
    	driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("總量管制")).click();
        
        String mainWin = driver.getWindowHandle();
        // open 2 windows for links to preserve the content of the main page
        driver.findElement(By.tagName("body")).sendKeys(Keys.CONTROL+"n");
        driver.findElement(By.tagName("body")).sendKeys(Keys.CONTROL+"n");
        
        String[] newWindows = new String[2]; 
        int nWin = 0;
        for(String hWnd : driver.getWindowHandles()) {
        	if(!hWnd.equals(mainWin)) {
        		newWindows[nWin] = hWnd;
        		nWin++;
        		if(nWin == 2)
        			break;
        	}
        }
        
        for(WebElement link: driver.findElements(By.partialLinkText("總量管制報表"))) {
        	// click the link with SHIFT pressed to open it in the new window
//        	new Actions(driver).keyDown(Keys.SHIFT).click(link).keyUp(Keys.SHIFT).perform();
        	String url = link.getAttribute("href");
        	String linkText = link.getText();
        	
        	if(url.endsWith(".zip")) continue;
        	if(url.endsWith("totalpoint.asp")) continue;
        	
            downloadTables(driver, "總量管制表", url, linkText, EnumSet.allOf(OutputType.class), newWindows);
//            downloadTables(driver, "總量管制表", url, linkText, EnumSet.of(OutputType.EXCEL), newWindows);
            driver.switchTo().window(mainWin);
        }
        
        driver.switchTo().window(newWindows[0]);
        driver.close();
        driver.switchTo().window(newWindows[1]);
        driver.close();
        driver.switchTo().window(mainWin);
        
        SortedMap<String, List<String>> tableUnits = new TreeMap<String, List<String>>();
        Set<String> unitSet = new HashSet<String>();
        
        Utils.obtain總量管制表單位對應表(tableUnits, unitSet);
        
        for(String unit: unitSet) {
    		new File(output_dir, "單位/"+unit+"/總量管制表").mkdir();
        }
        
        // Copy table files to the unit in charge according to the table-unit mapping
        for(String tableName: tableUnits.keySet()) {
        	if(tableUnits.get(tableName).isEmpty()) {
        		System.err.println("["+tableName+"]: no unit in charge");
        		continue;
        	}
        	
        	for(OutputType type: EnumSet.allOf(OutputType.class)) {
            	File tableFile = new File(output_dir, "總量管制表/"+type.name+"/"+tableName+type.ext);
            	for(String unit: tableUnits.get(tableName)) {
            		File unitDir = new File(output_dir, "單位/"+unit+"/總量管制表");
            		
            		try {
            			File targetFile = new File(unitDir, tableFile.getName());
            			
            			if(!targetFile.exists()) {
        					FileUtils.copyFile(tableFile, targetFile);
            			}
    					System.out.println("["+tableFile.getName()+"] => ["+unit+"]");
    				} catch (IOException e) {
    					System.err.println("["+tableFile.getName()+"] => ["+unit+"]: failed: "+e.getMessage());
    				}
            	}
        	}
        }
     
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void downloadTables(WebDriver driver, String folder, String startUrl, String currentLinkText, EnumSet<OutputType> types, String[] newWindows) throws InterruptedException {
		// Begin download
        
		for(OutputType type: types) {
			new File(output_dir, folder+"/"+type.name).mkdirs();
		}
		
    	Pattern patLink = Pattern.compile("\\d+-\\d+[A-Z]?");
    	//Pattern patHead = Pattern.compile("總量管制報表(一|二|三|四|五|六)");
    	
        // First, save the current page
        driver.switchTo().window(newWindows[0]);
        driver.get(startUrl);

        (new WebDriverWait(driver, 60)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        
		// Links with text that appear as ?-?A contain the table directly, ie., not a folder
		Matcher mlnk = patLink.matcher(currentLinkText);
		if(mlnk.find()) {
			for(OutputType type: types) {
				String finalName = mlnk.group() + type.ext;
				File finalOutput = new File(output_dir, folder+"/"+type.name+"/"+finalName);
				if(!finalOutput.exists()) {
					type.download(driver, finalOutput);
				}
			}
			return;
		}
//        for(WebElement h2 : driver.findElements(By.cssSelector("h2"))) {
//            String header = h2.getText();
//            Matcher m = patHead.matcher(header);
//            if(m.find()) {
//                for(OutputType type: types) {
//                    String finalName = m.group() + type.ext;
//                    File finalOutput = new File(output_dir, folder+"/"+type.name+"/"+finalName);
//                    if(!finalOutput.exists()) {
//                        type.download(driver, finalOutput);
//                    }
//                }
//                break;
//            }
//            else {
//                System.err.println("header ignored: "+header);
//            }
//        }
        
		
        // Click each link if not downloaded yet
        for (WebElement link : driver.findElements(By.partialLinkText("總量管制報表-"))) {
            Matcher m = patLink.matcher(link.getText());
        	if(!m.find()) {
        		System.err.println("link ignored: "+link.getText());
        		continue;
        	}
        	
        	String tableId = m.group();
        	
        	boolean pageLoaded = false;
        	
        	for(OutputType type: types) {
            	String finalName = tableId+type.ext;
            	File finalOutput = new File(output_dir, folder+"/"+type.name+"/"+finalName);
    			
            	if(finalOutput.exists())
            		continue;
            	
            	if(!pageLoaded) {
                	String url = link.getAttribute("href");
                    driver.switchTo().window(newWindows[1]);
                    driver.get(url);
                    
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
                driver.switchTo().window(newWindows[0]);
        	}
//            break;
		}
	}

    private static enum OutputType { 
    	EXCEL("Excel", ".xls", (driver) -> {
//			driver.findElement(By.partialLinkText("轉存Excel檔")).click();
			// With the above method, sometimes it hangs waiting for the response after clicking, while the file has already been saved
			((JavascriptExecutor)driver).executeScript(driver.findElement(By.partialLinkText("轉存Excel檔")).getAttribute("href"));
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
