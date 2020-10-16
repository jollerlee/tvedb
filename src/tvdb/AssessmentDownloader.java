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
        
        Utils.openTvdb(driver, "��Ų�򥻸�ƪ�");
        driver.findElement(By.partialLinkText("�C �L �t ��")).click();
        driver.findElement(By.partialLinkText("��ޮհ|��Ų��U")).click();
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        for(String hWnd : driver.getWindowHandles()) {
        	if(!hWnd.equals(mainWin)) {
        		newWin = hWnd;
        		break;
        	}
        }
        
        if(newWin == null) {
        	throw new NoSuchWindowException("��Ų�򥻸�ƪ�");
        }
        
        driver.close();
        driver.switchTo().window(newWin);
        mainWin = newWin;
        newWin = null;
        
        for(OutputType type: EnumSet.allOf(OutputType.class)) {
        	new File(output_dir, type.name+"/"+"��F��").mkdirs();
        	new File(output_dir, type.name+"/"+"�ѤH���U��").mkdirs();
        	new File(output_dir, type.name+"/"+"�@�z��").mkdirs();
        	new File(output_dir, type.name+"/"+"�Ƨ��~���ά�").mkdirs();
        }
        
        driver.findElement(By.partialLinkText("�u��F���v")).click();
        downloadTables(driver, "��F��", EnumSet.allOf(OutputType.class));
        
        driver.findElement(By.partialLinkText("�u�M�~����v")).click();
        
        Select unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("�ѤH���U��");
        Thread.sleep(1000); //TODO use something more robust instead
        downloadTables(driver, "�ѤH���U��", EnumSet.allOf(OutputType.class));

        unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("�@�z��");
        Thread.sleep(1000);
        downloadTables(driver, "�@�z��", EnumSet.allOf(OutputType.class));

        unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("�Ƨ��~���ά�");
        Thread.sleep(1000);
        downloadTables(driver, "�Ƨ��~���ά�", EnumSet.allOf(OutputType.class));
        
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void downloadTables(WebDriver driver, String folder, EnumSet<OutputType> types) throws InterruptedException {
		// Begin download
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        
        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.partialLinkText("��"));
        for (WebElement link : tables) {
        	String linkText = link.getText();
        	if(!linkText.matches("^\\s*��\\d+.*"))
        		continue;
        	
        	boolean pageLoaded = false;
        	
        	for(OutputType type: types) {
            	String finalName = linkText.replace('/', '��')+type.ext;
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
    	EXCEL("Excel", ".xls", "��sExcel", download_dir), 
    	PDF("PDF", ".pdf", "�����C�L", Utils.BULLZIP_DIR);
    	
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
