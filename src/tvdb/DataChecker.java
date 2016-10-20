package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		new File(output_dir, "�ˮ�").mkdirs();
		new File(output_dir, "���").mkdirs();
		
		WebDriver driver = Utils.createFireFoxDriver();
        
		Utils.openTvdb(driver, "����ˮ�");
        (new WebDriverWait(driver, 10)).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("�� �� �� ��")));
        driver.findElement(By.partialLinkText("�� �� �� ��")).click();

        // Cross-table checkers
        
        remove��������Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(1).click();
        
        waitFor��������Button(driver);
        
        downloadChecker(driver);
        
        // Other checkers
        
        remove��������Button(driver);
        
        driver.findElements(By.cssSelector("input[type='radio'][name='choose']")).get(2).click();
        
        waitFor��������Button(driver);
        
        downloadChecker(driver);
        
        // Start to copy checker result files to unit folders
        Map<String, List<String>> tableUnits = new HashMap<String, List<String>>();
        Map<String, List<String>> tableUnitsNonCurrent = new HashMap<String, List<String>>();
        Set<String> unitSet = new HashSet<String>();
        
		Utils.obtainTableUnitMapping(tableUnits, unitSet);
		Utils.obtain�D���TableUnitMapping(tableUnitsNonCurrent, unitSet);
		
        for(String unit: unitSet) {
    		new File(output_dir, "���/"+unit+"/�ˮֺøq").mkdirs();
        }
        
        tableUnits.putAll(tableUnitsNonCurrent);
        copyCheckResultToUnits(tableUnits);
        
        ((JavascriptExecutor)driver).executeScript("alert('Done! ��������e�ˮ֪��|�b�W�٤W�Щ��Ҧ�������U�A�гv���ˬd�C')");
        System.err.println("��������e�ˮ֪��|�b�W�٤W�Щ��Ҧ�������U�A�гv���ˬd�C");
	}

	private static void downloadChecker(WebDriver driver) {
		// The checker's select-tag reloads every time a item is clicked, thus has to be addressed by index
        Select checkers = new Select(driver.findElement(By.name("TabName")));
        int checkerCount = checkers.getOptions().size();
        
        checkers.selectByIndex(checkerCount-1); // click the last first to ensure the page is reloaded on selecting the first
        waitFor��������Button(driver);
        remove��������Button(driver);
        
        Pattern patternNoData = Pattern.compile("�Ӫ�U�L(��e)?�ˮְO��");
        
        for(int i=0; i<checkerCount; i++) {
            // remove a button and check its re-appearing as a sign of page loaded
            remove��������Button(driver);
            
            checkers = new Select(driver.findElement(By.name("TabName")));

            WebElement checker = checkers.getOptions().get(i);
        	String checkerName = checker.getText().trim().replace('/', '��');
        	checker.click();
        	
            waitFor��������Button(driver);
            
            String html = driver.findElement(By.tagName("body")).getText();
            if(!patternNoData.matcher(html).find()) {
            	// Has data
                File renamed = new File(output_dir, "�ˮ�/"+checkerName+".pdf");
                
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
                	output.renameTo(new File(output_dir, "�ˮ�/"+output.getName()));
                	continue;
                }
             
            }
        }
	}

	private static void copyCheckResultToUnits(Map<String, List<String>> tableUnits) {
		File[] checkers = new File(output_dir, "�ˮ�").listFiles();
		
		for(File checker: checkers) {
	        Set<String> units = new HashSet<String>();
	        
	        Utils.obtainUnitsOfChecker(checker.getName(), tableUnits, units);
	        
	        if(units.isEmpty()) {
	        	System.err.println("No unit-in-charge: ["+checker.getName()+"]");
	        	continue;
	        }
	        
	        for(String unit: units) {
	        	try {
					FileUtils.copyFileToDirectory(checker, new File(output_dir, "���/"+unit+"/�ˮֺøq"));
					System.out.println("["+checker.getName()+"] => ["+unit+"]");
				} catch (IOException e) {
					System.err.println("Error: ["+checker.getName()+"] => ["+unit+"]");
				}
	        }
		}
	}

	private static void waitFor��������Button(WebDriver driver) {
		(new WebDriverWait(driver, 10)).until(
        		ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='button'][value='��������']")));
	}

	private static void remove��������Button(WebDriver driver) {
		((JavascriptExecutor)driver).executeScript(
				"var inputs = document.getElementsByTagName('input');" +
				"for(var i=inputs.length-1; i>=0; i--) {" +
				"  if(inputs[i].type=='button' && inputs[i].value=='��������') {" +
				"    inputs[i].parentNode.removeChild(inputs[i]);" +
				"  }" +
				"}");
	}

}
