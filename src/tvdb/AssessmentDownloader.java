package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class AssessmentDownloader {

    private static final File output_dir = Utils.ASSESS_DIR;
    private static final File download_dir = new File(output_dir, "temp");
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		
        FirefoxProfile profile = Utils.createFireFoxProfile();
        profile.setPreference("browser.download.dir", download_dir.getPath());
        
        WebDriver driver = Utils.createFireFoxDriver(profile);
        
        Utils.openTvdb(driver, "評鑑基本資料表");
        driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("科技校院評鑑表冊")).click();
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        for(String hWnd : driver.getWindowHandles()) {
        	if(!hWnd.equals(mainWin)) {
        		newWin = hWnd;
        		break;
        	}
        }
        
        if(newWin == null) {
        	throw new NoSuchWindowException("評鑑基本資料表");
        }
        
        driver.close();
        driver.switchTo().window(newWin);
        mainWin = newWin;
        newWin = null;
        
        for(OutputType type: EnumSet.allOf(OutputType.class)) {
        	new File(output_dir, type.name+"/"+"行政類").mkdirs();
        	new File(output_dir, type.name+"/"+"老人照顧科").mkdirs();
        	new File(output_dir, type.name+"/"+"護理科").mkdirs();
        	new File(output_dir, type.name+"/"+"化妝品應用科").mkdirs();
        }
        
        driver.findElement(By.partialLinkText("「行政類」")).click();
        downloadTables(driver, "行政類", EnumSet.allOf(OutputType.class));
        
        driver.findElement(By.partialLinkText("「專業類科」")).click();
        
        Select unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("老人照顧科");
        Thread.sleep(1000); //TODO use something more robust instead
        downloadTables(driver, "老人照顧科", EnumSet.allOf(OutputType.class));

        unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("護理科");
        Thread.sleep(1000);
        downloadTables(driver, "護理科", EnumSet.allOf(OutputType.class));

        unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("化妝品應用科");
        Thread.sleep(1000);
        downloadTables(driver, "化妝品應用科", EnumSet.allOf(OutputType.class));
        
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void downloadTables(WebDriver driver, String folder, EnumSet<OutputType> types) throws InterruptedException {
		// Begin download
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        
        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.partialLinkText("表"));
        for (WebElement link : tables) {
        	String linkText = link.getText();
        	if(!linkText.matches("^\\s*表\\d+.*"))
        		continue;
        	
        	boolean pageLoaded = false;
        	
        	for(OutputType type: types) {
            	String finalName = linkText.replace('/', '及')+type.ext;
            	File finalOutput = new File(output_dir, type.name+"/"+folder+"/"+finalName);
    			
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
    	EXCEL("Excel", ".xls", "轉存Excel", download_dir), 
    	PDF("PDF", ".pdf", "按此列印", Utils.BULLZIP_DIR);
    	
    	public final String name;
    	public final String ext;
		private String buttonName;
		private File generatedFileFolder;
    	
    	OutputType(String name, String ext, String buttonName, File genFileFolder) {
    		this.name = name;
    		this.ext = ext;
    		this.buttonName = buttonName;
    		this.generatedFileFolder = genFileFolder;
    		
    		generatedFileFolder.mkdirs();
    		
	        for(File f: generatedFileFolder.listFiles()) {
	        	if(f.isFile()) {
	        		f.delete();
	        	}
	        }
    	}
    	
    	void download(WebDriver driver, File output) {
    		driver.findElement(By.cssSelector("input[type='button'][value='"+buttonName+"']")).click();
			// With the above method, sometimes it hangs waiting for the response after clicking, while the file has already been saved.
    		// See AmountControlDataDownloader.java for an alternative way.

    		File newFile = Utils.waitForGeneratedFile(generatedFileFolder);
    		
            if(!newFile.renameTo(output)) {
            	System.out.println("Failed to rename ["+newFile.getName()+"] to "+output.getPath());
            	newFile.renameTo(new File(output.getParentFile(), newFile.getName()));
            }
    	}
    };
}
