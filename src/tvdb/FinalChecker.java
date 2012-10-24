package tvdb;
import java.io.IOException;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class FinalChecker {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
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

		(new WebDriverWait(driver, 10)).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("輸入表冊一覽表")));
		driver.findElement(By.partialLinkText("輸入表冊一覽表")).click();
		(new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

		// 表冊資訊
		List<WebElement> trs = driver.findElements(By.tagName("table")).get(0).findElements(By.tagName("tr"));
		trs.remove(0); // remove table header

		for (WebElement tr : trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
			String tableName = tds.get(1).getText();
			String unitStr = tds.get(5).getText().trim();
			if(unitStr.isEmpty()) {
				System.err.println("["+tableName+"]: 未設定填報單位");
			}
			if(tds.get(6).getText().trim().equals("無") && tds.get(7).getText().trim().isEmpty()) {
				System.err.println("["+tableName+"]: 尚未確認無資料");
			}
		}
		
		// 表冊檢核明細
		trs = driver.findElements(By.tagName("table")).get(1).findElements(By.tagName("tr"));
		trs.remove(0); // remove table primary header
		trs.remove(0); // remove table secondary header

		for (WebElement tr : trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
			String checkerIdx = tds.get(0).getText();
			String checkerName = tds.get(2).getText();
			String unitStr = tds.get(5).getText().trim();
			if(unitStr.isEmpty()) {
				System.err.println("("+checkerIdx+") ["+checkerName+"]: 未設定檢核單位");
			}
			Select checked = new Select(tds.get(6).findElement(By.tagName("select")));
			if(checked.getFirstSelectedOption().getAttribute("value").equals("n")) {
				System.err.println("("+checkerIdx+") ["+checkerName+"]: 未完成檢核");
			}
		}
		
        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

}
