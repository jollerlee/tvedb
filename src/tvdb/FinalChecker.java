package tvdb;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
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
		String mainUrl = driver.getCurrentUrl();

		// 未填表冊
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR) - 1911;
		int month = now.get(Calendar.MONTH) > 7 ? 10 : 3;
		
		driver.findElement(By.partialLinkText("資 料 檢 核")).click();
		
		int currentTableIdx = 30;
		
		eachTvdbTable:
		while(true) {
			Select tabSelect = new Select(driver.findElement(By.cssSelector("select[name='TabName']")));
			int tableCount = tabSelect.getOptions().size();

			String tableName = tabSelect.getFirstSelectedOption().getAttribute("value");
			
			// look for the correct HTML table
			for(WebElement htmlTable: driver.findElements(By.tagName("table"))) {
				List<WebElement> trs = htmlTable.findElements(By.tagName("tr"));
				
				if(trs.size() == 0)
					continue;
				
				List<WebElement> tableHeaderTds = trs.get(0).findElements(By.tagName("td"));
				
				if(tableHeaderTds.size() >= 5 && tableHeaderTds.get(2).getText().equals("表冊未填原因")) {
					trs.remove(0); // remove table header
					
					// look for not-yet-confirmed records
					for(WebElement tr: trs) {
						List<WebElement> tds = tr.findElements(By.tagName("td"));
						WebElement confirmChkBox = tds.get(3).findElement(By.cssSelector("input[type='checkbox']"));
						if(confirmChkBox.isEnabled()) {
							// a record found to be not-yet-confirmed 
							if(tds.get(4).getText().equals(""+year+":"+month)) {
								// 當期
								if(new File(Utils.TVDB_DIR, "基本資料表/"+tableName+"-無資料.txt").exists()) {
									// 確認過無資料者
									System.out.println("當期:無資料: ["+tableName+"]: => 確認");
									confirmChkBox.click();
									confirmChkBox.submit();
									driver.switchTo().alert().accept();
									continue eachTvdbTable; // restart the outermost loop since the form has been submitted
								}
								else {
									System.err.println("當期:無資料: ["+tableName+"]: => 尚未確認");
								}
							}
							else {
								System.err.println("歷史:無資料: ["+tableName+"]["+tds.get(4).getText()+"]");
							}
						}
					}
					break;
				}
			}
			
			currentTableIdx++;
			
			if(currentTableIdx >= tableCount)
				break;
			
			// change to the next table
			
			remove關閉視窗Button(driver);
			tabSelect.selectByIndex(currentTableIdx);
			waitFor關閉視窗Button(driver);
		}

		// 表冊及檢核總覽
		
		driver.get(mainUrl);
		driver.findElement(By.partialLinkText("輸入表冊一覽表")).click();

		boolean allOk = true;
		boolean changed = false;
		
		// 表冊資訊
		List<WebElement> trs = driver.findElements(By.tagName("table")).get(0).findElements(By.tagName("tr"));
		trs.remove(0); // remove table header
		
		int trIdx = 1;

		for (WebElement tr : trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
			String tableName = tds.get(1).getText();
			String unitStr = tds.get(5).getText().trim();
			if(unitStr.isEmpty()) {
				System.err.println("["+tableName+"]: 未設定填報單位");
				allOk = false;
				((JavascriptExecutor)driver).executeScript("document.getElementsByTagName('table')[0].getElementsByTagName('tr')["+trIdx+"]." +
						"getElementsByTagName('td')[5].style.backgroundColor='#ff0000';");
			}
			if(tds.get(6).getText().trim().equals("無") && tds.get(7).getText().trim().isEmpty()) {
				System.err.println("["+tableName+"]: 尚未確認無資料");
				allOk = false;
				((JavascriptExecutor)driver).executeScript("document.getElementsByTagName('table')[0].getElementsByTagName('tr')["+trIdx+"]." +
						"getElementsByTagName('td')[7].style.backgroundColor='#ff0000';");
			}
			trIdx++;
		}
		
		// 表冊檢核明細
		trs = driver.findElements(By.tagName("table")).get(1).findElements(By.tagName("tr"));
		trs.remove(0); // remove table primary header
		trs.remove(0); // remove table secondary header
		
		trIdx = 2;

		for (WebElement tr : trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
			String checkerIdx = tds.get(0).getText();
			String checkerName = tds.get(2).getText();
			String unitStr = tds.get(5).getText().trim();
			if(unitStr.isEmpty()) {
				System.err.println("("+checkerIdx+") ["+checkerName+"]: 未設定檢核單位");
				allOk = false;
				((JavascriptExecutor)driver).executeScript("document.getElementsByTagName('table')[1].getElementsByTagName('tr')["+trIdx+"]." +
						"getElementsByTagName('td')[5].style.backgroundColor='#ff0000';");
			}
			Select checked = new Select(tds.get(6).findElement(By.tagName("select")));
			if(checked.getFirstSelectedOption().getAttribute("value").equals("n")) {
				checked.selectByValue("y");
				System.out.println("("+checkerIdx+") ["+checkerName+"]: 設定完成檢核");
				changed = true;
			}
			trIdx++;
		}
		
		if(allOk) {
			if(changed) {
				driver.findElement(By.cssSelector("input[type='submit'][value='確定儲存']")).click();
		        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
			}
			else {
		        ((JavascriptExecutor)driver).executeScript("alert('Nothing to do.')");
			}
		}
		else {
	        ((JavascriptExecutor)driver).executeScript("alert('請修改錯誤後, 按下方  <確定儲存> 按鈕')");
		}
	}

	private static void waitFor關閉視窗Button(WebDriver driver) {
		(new WebDriverWait(driver, 10)).until(
        		ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='button'][value='關閉視窗']")));
	}

	private static void remove關閉視窗Button(WebDriver driver) {
		((JavascriptExecutor)driver).executeScript(
				"var inputs = document.getElementsByTagName('input');" +
				"for(var i=inputs.length-1; i>=0; i--) {" +
				"  if(inputs[i].type=='button' && inputs[i].value=='關閉視窗') {" +
				"    inputs[i].parentNode.removeChild(inputs[i]);" +
				"  }" +
				"}");
	}

}
