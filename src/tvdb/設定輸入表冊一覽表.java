package tvdb;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class 設定輸入表冊一覽表 {

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException,
			IOException {
		WebDriver driver = new FirefoxDriver(Utils.createFireFoxProfile());

		Utils.openTvdb(driver, null);
		(new WebDriverWait(driver, 10)).until(ExpectedConditions
				.elementToBeClickable(By.partialLinkText("輸入表冊一覽表")));
		driver.findElement(By.partialLinkText("輸入表冊一覽表")).click();

		// Setup units of tables
		(new WebDriverWait(driver, 30)).until(ExpectedConditions
				.presenceOfElementLocated(By.tagName("table")));

		SortedMap<String, List<String>> tableUnits = new TreeMap<String, List<String>>();
		Set<String> unitSet = new HashSet<String>();

		Utils.obtainTableUnitMapping(driver, tableUnits, unitSet);

		// Read tables filler table and construct the mapping
		List<WebElement> trs = driver.findElement(By.tagName("table"))
				.findElements(By.tagName("tr"));
		
		trs.remove(0); // remove table header
		
		for(WebElement tr: trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
			String tableName = tds.get(1).getText();
			tableName = tableName.replaceFirst("^table", "").replace("_", "-");
			WebElement unitTextField = tds.get(5).findElement(By.tagName("textarea"));
			unitTextField.clear();
			unitTextField.sendKeys(StringUtils.join(tableUnits.get(tableName), ","));
		}

		// 表冊檢核明細
		Utils.obtain非當期TableUnitMapping(driver, tableUnits, unitSet);
		trs = driver.findElements(By.tagName("table")).get(1).findElements(By.tagName("tr"));
		trs.remove(0); // remove table primary header
		trs.remove(0); // remove table secondary header
		
		for (WebElement tr : trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
//			String checkerIdx = tds.get(0).getText();
			String checkerName = tds.get(2).getText();
			Set<String> units = new HashSet<String>();
			
			Utils.obtainUnitsOfChecker(checkerName, tableUnits, units);
			
			WebElement unitTextField = tds.get(5).findElement(By.tagName("textarea"));
			unitTextField.clear();
			unitTextField.sendKeys(StringUtils.join(units, ","));
		}
		((JavascriptExecutor) driver).executeScript("alert('請在確認無誤後按下 [儲存], 並留意本頁是否有[會計表冊]須處理!')");
	}

}
