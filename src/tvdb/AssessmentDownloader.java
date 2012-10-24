package tvdb;
import java.io.File;
import java.util.EnumSet;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class AssessmentDownloader {

    private static final File output_dir = new File("D:/work/��Ų/download");
    private static final File download_dir = new File(output_dir, "temp");
    
    private static enum OutputType { 
    	EXCEL("Excel", ".xls", "��sExcel", download_dir), 
    	PDF("PDF", ".pdf", "�����C�L", Utils.BULLZIP_DIR);
    	
//    	public final String name;
    	public final String ext;
		private String buttonName;
		private File outputFolder;
		private File generatedFileFolder;
    	
    	OutputType(String name, String ext, String buttonName, File genFileFolder) {
//    		this.name = name;
    		this.ext = ext;
    		this.buttonName = buttonName;
    		this.outputFolder = new File(output_dir, name);
    		this.generatedFileFolder = genFileFolder;
    		
    		outputFolder.mkdirs();
    		generatedFileFolder.mkdirs();
    		
	        for(File f: generatedFileFolder.listFiles()) {
	        	if(f.isFile()) {
	        		f.delete();
	        	}
	        }
    	}
    	
    	void download(WebDriver driver, String fileName) {
    		driver.findElement(By.cssSelector("input[type='button'][value='"+buttonName+"']")).click();
    		File newFile = Utils.waitForGeneratedFile(generatedFileFolder);
    		File finalFile = new File(outputFolder, fileName);
    		finalFile.getParentFile().mkdirs();
    		
            if(!newFile.renameTo(finalFile)) {
            	System.out.println("Failed to rename ["+newFile.getName()+"] to "+finalFile.getPath());
            	newFile.renameTo(new File(finalFile.getParentFile(), newFile.getName()));
            }
    	}
    	
    	File getOutputFolder() {
    		return this.outputFolder;
    	}
    };
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
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
		
        Utils.openTvdb(driver, "��Ų�򥻸�ƪ�");
        driver.findElement(By.partialLinkText("�C �L �t ��")).click();
        driver.findElement(By.partialLinkText("��Ų�򥻸�ƪ�")).click();
        
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
        
        driver.findElement(By.partialLinkText("�u��F���v")).click();
        downloadTables(driver, "��F��", EnumSet.allOf(OutputType.class));
        
        driver.findElement(By.partialLinkText("�u�M�~����v")).click();
        
        Select unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("�ѤH���U��");
        Thread.sleep(1000); //TODO use something more robust instead
        downloadTables(driver, "�M�~����-�ѤH���U��", EnumSet.allOf(OutputType.class));

        unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("�@�z��");
        Thread.sleep(1000);
        downloadTables(driver, "�M�~����-�@�z��", EnumSet.allOf(OutputType.class));

        unit = new Select(driver.findElement(By.id("setUnits")));
        unit.selectByVisibleText("�Ƨ��~���ά�");
        Thread.sleep(1000);
        downloadTables(driver, "�M�~����-�Ƨ��~���ά�", EnumSet.allOf(OutputType.class));
        
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
            	String finalName = folder+"/"+linkText.replace('/', '��')+type.ext;
    			
            	if(new File(type.getOutputFolder(), finalName).exists())
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
            	
            	type.download(driver, finalName);
                
        	}
        	if(pageLoaded) {
                driver.close();
                driver.switchTo().window(mainWin);
        	}
//            break;
		}
	}

}
