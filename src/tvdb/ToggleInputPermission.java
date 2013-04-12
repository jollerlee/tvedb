package tvdb;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;


public class ToggleInputPermission {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		boolean enableInput;
		
		while(true) {
			System.out.println("Want to (1) Enable or (2) Disable input permission?");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = br.readLine();
			
			try {
				int choice = Integer.parseInt(line);
				
				if(choice == 1) {
					enableInput = true;
					break;
				}
				else if(choice == 2) {
					enableInput = false;
					break;
				}
			}
			catch (NumberFormatException e) {
				System.err.println("Illegal choice; please input 1 or 2");
			}
		}
		
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
		
		Utils.openTvdb(driver, null);
		
        driver.findElement(By.partialLinkText("權 限 管 理")).click();
        driver.findElement(By.partialLinkText("校內帳號一覽表")).click();
        
        String mainWin = driver.getWindowHandle();
        String newWin = null;
        
        for(String hWnd : driver.getWindowHandles()) {
        	if(!hWnd.equals(mainWin)) {
        		newWin = hWnd;
        		break;
        	}
        }
        
        driver.switchTo().window(newWin);
        
        // Start to change the permission.
        // Note that every time the button is clicked, the form is submitted, and thus the WebElement instances become invalid.
        // We have to remember some id's and names in order to traverse all controls.
        
        List<WebElement> 變更按鈕們 = driver.findElements(By.cssSelector("input[type='submit'][value='變更']"));
        List<String> buttonIds = new ArrayList<String>();
        List<String> chkboxNames = new ArrayList<String>();
        
        // First, remember the button ID's
        for(WebElement button: 變更按鈕們) {
        	buttonIds.add(button.getAttribute("id"));
        }
        
        // Next, remember the names of the input-permission checkboxes
        List<WebElement> chkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
        for(WebElement chkbox: chkboxes) {
        	if(chkbox.getAttribute("name").matches("input_yn.*")) {
        		chkboxNames.add(chkbox.getAttribute("name"));
        	}
        }
        
        // Finally, click the buttons
        for(String buttonId: buttonIds) {
    		WebElement chkbox = driver.findElement(By.name("input_yn_"+buttonId));
    		if(chkbox.isSelected() != enableInput) {
    			chkbox.click();
    		}
        	
        	// Do click the button. Simply clicking the button doesn't work except for IE, so we have to work it around. 
        	((JavascriptExecutor)driver).executeScript("document.getElementsByName('cid')[0].value = '"+buttonId+"';");
        	driver.findElement(By.id(buttonId)).click();
        }
        
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

}
