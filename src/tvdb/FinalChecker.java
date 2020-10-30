package tvdb;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
		String startTable;

		if(!Files.isDirectory(Utils.getNoDataReasonDir())) {
		    System.err.println("The folder `"+Utils.getNoDataReasonDir()+"' doesn't exist!");
		    System.exit(1);
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			System.out.println("Start from which table? (0 for the first table; ?_? for table ?_?)");
			String line = br.readLine().trim();
			if(line.equals("0")) {
				startTable = null;
			}
			else if(line.matches("\\d+_\\d+(_\\d+)?")) {
				startTable = "table"+line;
			}
			else {
				continue;
			}
			break;
		}
		
		WebDriver driver = Utils.createFireFoxDriver();
        
		Utils.openTvdb(driver, null);
		String mainUrl = driver.getCurrentUrl();

		// �����U
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR) - 1911;
		int month = now.get(Calendar.MONTH) > 7 ? 10 : 3;
		
		driver.findElement(By.partialLinkText("�� �� �� ��")).click();
		
		int currentTableIdx = 0;
		
		waitFor��������Button(driver);
		Select tabSelect = new Select(driver.findElement(By.cssSelector("select[name='TabName']")));

		if(startTable != null) {
			for(WebElement tableName: tabSelect.getOptions()) {
				if(tableName.getAttribute("value").equals(startTable)) {
					break;
				}
				currentTableIdx++;
			}
			
			if(currentTableIdx == tabSelect.getOptions().size()) {
				System.err.println("start table not found; starting from the first one");
				currentTableIdx = 0;
			}
			else {
				remove��������Button(driver);
				tabSelect.selectByIndex(currentTableIdx);
				waitFor��������Button(driver);
				tabSelect = new Select(driver.findElement(By.cssSelector("select[name='TabName']")));
			}
		}
		
		eachTvdbTable:
		while(true) {
//			try {
			int tableCount = tabSelect.getOptions().size();
			String tableName = tabSelect.getFirstSelectedOption().getText();
			tableName = tableName.toLowerCase().replace("table", "").replace('_', '-');
			
			boolean dataTableFound = false;
			
			// look for the correct HTML table
			for(WebElement htmlTable: driver.findElements(By.tagName("table"))) {
				List<WebElement> trs = htmlTable.findElements(By.tagName("tr"));
				
				if(trs.size() == 0)
					continue;
				
				List<WebElement> tableHeaderTds = trs.get(0).findElements(By.tagName("td"));
				
				if(tableHeaderTds.size() >= 5 && tableHeaderTds.get(2).getText().equals("��U�����]")) {
					dataTableFound = true;
					trs.remove(0); // remove table header
					
					// look for not-yet-confirmed records
					for(WebElement tr: trs) {
						List<WebElement> tds = tr.findElements(By.tagName("td"));
						WebElement confirmChkBox = tds.get(3).findElement(By.cssSelector("input[type='checkbox']"));
						if(confirmChkBox.isEnabled()) {
							// a record found to be not-yet-confirmed 
							if(tds.get(4).getText().equals(""+year+":"+month)) {
								// ���
							    Path reasonFile = Utils.getNoDataReasonDir().resolve(tableName+".txt");
								if(Files.exists(reasonFile)) {
									// �T�{�L�L��ƪ�
									String reason = String.join(" ", Files.readAllLines(reasonFile));
                                    System.out.println("���:�L���: ["+tableName+"]: => "+reason);
                                    
									WebElement reasonInput = tds.get(2).findElement(By.cssSelector("input"));
									reasonInput.clear();
									reasonInput.sendKeys(reason);
								}
								else {
									System.err.println("���:�L���: ["+tableName+"]: => default reason");
								}
								
                                confirmChkBox.click();
                                confirmChkBox.submit();
                                driver.switchTo().alert().accept();
                                tabSelect = new Select(driver.findElement(By.cssSelector("select[name='TabName']")));
                                continue eachTvdbTable; // restart the outermost loop since the form has been submitted
							}
							else {
								System.err.println("���v:�L���: ["+tableName+"]["+tds.get(4).getText()+"]");
							}
						}
					}
					break;
				}
			}
			
			if(!dataTableFound) {
				System.err.println("["+tableName+"]: Data table not found");
			}
			
			// handle the tricky case: sometimes the options duplicate
			String lastTableName = tableName;
			do {
				currentTableIdx++;

				if(currentTableIdx >= tableCount)
					break eachTvdbTable;
				
				tableName = tabSelect.getOptions().get(currentTableIdx).getAttribute("value").replace("table", "").replace('_', '-');
			} while(lastTableName.equals(tableName));
			
			// change to the next table
			
			remove��������Button(driver);
			tabSelect.selectByIndex(currentTableIdx);
			waitFor��������Button(driver);
			tabSelect = new Select(driver.findElement(By.cssSelector("select[name='TabName']")));
//			}
//			catch (UnreachableBrowserException e) {
//				if(e.getCause() instanceof BindException) {
//					System.err.println("Binding failed; wait for 10 seconds");
//					Thread.sleep(30000);
//					
//					Select tableSelect = new Select(driver.findElement(By.cssSelector("select[name='TabName']")));
//					String currentOptionValue = tableSelect.getFirstSelectedOption().getAttribute("value");
//					
//					// sync select index
//					int i=0;
//					for(WebElement option: tableSelect.getOptions()) {
//						if(currentOptionValue.equals(option.getAttribute("value"))) {
//							currentTableIdx = i;
//							break;
//						}
//						i++;
//					}
//					
//					(new WebDriverWait(driver, 10)).until(
//			        		ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='submit'][value='�e�X']")));
//				}
//				else {
//					throw e;
//				}
//			}
		}

		// ��U���ˮ��`��
		
		driver.get(mainUrl);
		driver.findElement(By.partialLinkText("��J��U�@����")).click();

		boolean allOk = true;
		boolean changed = false;
		
		// ��U��T
		List<WebElement> trs = driver.findElements(By.tagName("table")).get(0).findElements(By.tagName("tr"));
		trs.remove(0); // remove table header
		
		int trIdx = 1;

		for (WebElement tr : trs) {
			List<WebElement> tds = tr.findElements(By.tagName("td"));
			String tableName = tds.get(1).getText();
			String unitStr = tds.get(5).getText().trim();
			if(unitStr.isEmpty()) {
				System.err.println("["+tableName+"]: ���]�w������");
				allOk = false;
				((JavascriptExecutor)driver).executeScript("document.getElementsByTagName('table')[0].getElementsByTagName('tr')["+trIdx+"]." +
						"getElementsByTagName('td')[5].style.backgroundColor='#ff0000';");
			}
			if(tds.get(6).getText().trim().equals("�L") && tds.get(7).getText().trim().isEmpty()) {
				System.err.println("["+tableName+"]: �|���T�{�L���");
				allOk = false;
				((JavascriptExecutor)driver).executeScript("document.getElementsByTagName('table')[0].getElementsByTagName('tr')["+trIdx+"]." +
						"getElementsByTagName('td')[7].style.backgroundColor='#ff0000';");
			}
			trIdx++;
		}
		
		// ��U�ˮ֩���
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
				System.err.println("("+checkerIdx+") ["+checkerName+"]: ���]�w�ˮֳ��");
				allOk = false;
				((JavascriptExecutor)driver).executeScript("document.getElementsByTagName('table')[1].getElementsByTagName('tr')["+trIdx+"]." +
						"getElementsByTagName('td')[5].style.backgroundColor='#ff0000';");
			}
			Select checked = new Select(tds.get(6).findElement(By.tagName("select")));
			if(checked.getFirstSelectedOption().getAttribute("value").equals("n")) {
				checked.selectByValue("y");
				System.out.println("("+checkerIdx+") ["+checkerName+"]: �]�w�����ˮ�");
				changed = true;
			}
			trIdx++;
		}
		
		if(allOk) {
			if(changed) {
				driver.findElement(By.cssSelector("input[type='submit'][value='�T�w�x�s']")).click();
		        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
			}
			else {
		        ((JavascriptExecutor)driver).executeScript("alert('Nothing to do.')");
			}
		}
		else {
	        ((JavascriptExecutor)driver).executeScript("alert('�Эק���~��, ���U��  <�T�w�x�s> ���s')");
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
