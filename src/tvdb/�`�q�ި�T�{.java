package tvdb;

import java.awt.AWTException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class 總量管制確認 {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws AWTException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
//		WebDriver driver = new FirefoxDriver();
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
		WebDriver driver = new InternetExplorerDriver(capabilities);
        
		Utils.openTvdb(driver, null);
    	
    	driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("總量管制")).click();
        
        List<Link> links = new LinkedList<Link>();
        
        for(WebElement link: driver.findElements(By.partialLinkText("總量管制報表"))) {
        	links.add(new Link(link.getAttribute("href"), link.getText()));
        }
        
        for(Link link: links) {
        	// click the link with SHIFT pressed to open it in the new window
//        	new Actions(driver).keyDown(Keys.SHIFT).click(link).keyUp(Keys.SHIFT).perform();
            confirmTables(driver, link);
        }
        
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

	private static void confirmTables(WebDriver driver, Link startLink) throws InterruptedException {
		// Begin download
        
    	Pattern pat = Pattern.compile("^總量管制報表-表\\d+-\\d+[A-Z]?");

    	if(!pat.matcher(startLink.getText()).find()) {
        	return;
        }
    	
        // First, confirm the current page
        driver.get(startLink.getUrl());
        
        doConfirm(driver, startLink.getText());

        // Click each link if not done yet
        List<Link> links = new LinkedList<Link>();
        
        for(WebElement link: driver.findElements(By.partialLinkText("總量管制報表-"))) {
        	links.add(new Link(link.getAttribute("href"), link.getText()));
        }
        
        for(Link link: links) {
        	Matcher m = pat.matcher(link.getText());
        	if(!m.find()) {
        		System.err.println("link ignored: "+StringUtils.abbreviate(link.getText(), 50));
        		continue;
        	}
        	
            driver.get(link.getUrl());
            
            (new WebDriverWait(driver, 60)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
            doConfirm(driver, link.getText());
		}
	}

	private static void doConfirm(WebDriver driver, String linkText) throws InterruptedException {
    	Set<String> originalWindows = new HashSet<String>(driver.getWindowHandles());
    	String currentWin = driver.getWindowHandle();
        
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				if (!d.findElements(By.cssSelector("input[type='button'][value^='請確認']")).isEmpty()) {
					return true;
				} 
				else if (!d.findElements(By.cssSelector("input[type='button'][disabled]")).isEmpty()) {
					return true;
				}
				else {
					return false;
				}
			}
		});
		
    	List<WebElement> confirmButtons = driver.findElements(By.cssSelector("input[type='button'][value^='請確認']"));
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        
        if(confirmButtons.isEmpty()) {
        	System.err.println("No button found: "+linkText);
        	return;
        }
        else if(confirmButtons.size() > 1) {
        	System.err.println("Multiple buttons found: "+linkText);
        	return;
        }
        
        confirmButtons.get(0).click();
        
        while(driver.getWindowHandles().size() != originalWindows.size()+1) {
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
        }
        
        String newWin = null;
        for(String handle: driver.getWindowHandles()) {
        	if(!originalWindows.contains(handle)) {
            	// New window found
            	newWin = handle;
            	break;
        	}
        }
        
        if(newWin == null) {
        	System.err.println("No new window found after clicking the button: "+linkText);
        }
        
        driver.switchTo().window(newWin);
        
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        if(!driver.findElements(By.id("overridelink")).isEmpty()) {
        	driver.navigate().to("javascript:document.getElementById('overridelink').click()");
        }
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        
        driver.findElement(By.cssSelector("input[type='submit'][value='確認']")).click();
//        ((JavascriptExecutor)driver).executeScript("document.getElementById('check').action += '?kind=save'");
//        driver.findElement(By.cssSelector("input[type='submit'][value='確認']")).submit();
        driver.switchTo().alert().accept();
        driver.switchTo().window(currentWin);
        System.out.println("Confirmed: "+linkText);
	}

	private static class Link {
		private String url;
		public String getUrl() {
			return url;
		}

		public String getText() {
			return text;
		}

		private String text;
		
		public Link(String url, String text) {
			this.url = url;
			this.text = text;
		}
	}
}
