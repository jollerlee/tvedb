package tvdb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Utils {

	static final File BULLZIP_DIR = new File("D:/temp/Bullzip");
	static final File TVDB_DIR = new File("D:/work/tvdb/download");
	static final File TVDB_WORK_DIR = new File("D:/work/tvdb/work");
	static final File ASSESS_DIR = new File("D:/work/評鑑/download");

	public static void main(String[] args) throws IOException {
		compressAll(new File(TVDB_DIR, "單位"));
	}

	static File waitForGeneratedFile(File outputPath) {
		while (true) {
			File[] files = outputPath.listFiles(new FileFilter() {
				public boolean accept(File f) {
					if (!f.isFile())
						return false;
					else if (f.length() == 0)
						return false;
					else if (f.getName().endsWith(".part"))
						return false;
					else
						return true;
				}
			});

			if (files.length == 0) {
				try {
					Thread.sleep(300);
				} catch (InterruptedException ignored) {
				}
				continue;
			}

			if (files.length > 1) {
				System.err.println("More than one output file");
			}

			return files[files.length - 1];
		}
	}

	static void compressAll(File dir) throws IOException {
		for (File subdir : dir.listFiles()) {
			if (subdir.isFile())
				continue;

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(dir, subdir.getName()
							+ ".zip"))), Charset.forName("big5"));
			for (File file : subdir.listFiles()) {
				out.putNextEntry(new ZipEntry(subdir.getName() + "/"
						+ file.getName()));

				int c;
				byte[] buf = new byte[1024];
				InputStream is = new BufferedInputStream(new FileInputStream(
						file));
				while ((c = is.read(buf)) != -1) {
					out.write(buf, 0, c);
				}

				is.close();
			}

			out.close();
		}
	}

	static void openTvdb(WebDriver driver, String linkText) {
		// Remove old temp files
		for (File f : BULLZIP_DIR.listFiles()) {
			if (f.isFile()) {
				f.delete();
			}
		}

		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		Dimension winSize = driver.manage().window().getSize();
		Dimension newWinSize = new Dimension(1700, winSize.height);
		driver.manage().window().setSize(newWinSize);

		// Don't know why, but document.write seems to hang
		// ((JavascriptExecutor)driver).executeScript(
		// "document.write('<p>請確認  Bullzip printer 輸出路徑已設為 "+output_dir.getPath().replace('\\',
		// '/')+"/&lt;time&gt;.pdf<br>" +
		// "確認後請按以下 link 開始下載表冊:<br>" +
		// "<a href=\"http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp\">評鑑基本資料表</a>');"
		// +
		// "document.close();");

		if (linkText == null) {
			driver.get("http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp");
		} else {
			((JavascriptExecutor) driver)
					.executeScript("var p = document.createElement('p');"
							+ "p.innerHTML= '請確認  Bullzip printer 輸出路徑已設為 "
							+ BULLZIP_DIR.getPath().replace('\\', '/')
							+ "/&lt;time&gt;.pdf<br>"
							+ "確認後請按以下 link 開始下載表冊:<br>';"
							+ "var a = document.createElement('a');"
							+ "a.setAttribute('href', 'http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp');"
							+ "a.innerHTML = '"
							+ linkText
							+ "';"
							+ "p.appendChild(a);"
							+ "document.getElementsByTagName('body')[0].appendChild(p);");

			// Pop-ups cause subsequent call to WebDriver throwing exception
			// ((JavascriptExecutor)driver).executeScript(
			// "confirm('請確認  Bullzip printer 輸出路徑已設為 ["+output_dir.getPath().replace('\\',
			// '/')+"]<time>.pdf');");
			// ((JavascriptExecutor)driver).executeScript(
			// "document.location.href='http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp';");

			// Wait for the user clicking the link
			(new WebDriverWait(driver, 100000000))
					.until(new ExpectedCondition<Boolean>() {

						public Boolean apply(WebDriver drv) {
							return drv
									.getCurrentUrl()
									.equals("http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp");
						}

					});
		}

		driver.findElement(By.linkText("技專校院")).click();

		// wait for VPN login page
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				if (d.getCurrentUrl().contains("welcome.cgi")) {
					return true;
				} else {
					return false;
				}
			}
		});
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		// input VPN auth info
		WebElement username = driver.findElement(By.name("username"));
		username.sendKeys("100910");
		driver.findElement(By.name("password")).sendKeys("3453");
		username.submit();

		driver.findElement(By.linkText("技專校院校務基本資料庫")).click();
		driver.findElement(By.linkText("技專校院")).click();

		// wait for system login page
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				if (d.getCurrentUrl().contains("login_school.htm")) {
					return true;
				} else {
					return false;
				}
			}
		});
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		// login system
		username = driver.findElement(By.name("user_id"));
		username.sendKeys("ntin");
		driver.findElement(By.name("user_pwd")).sendKeys("ntinlic");
		username.submit();
	}

	static void obtainTableUnitMapping(WebDriver driver,
			Map<String, List<String>> tableUnits, Set<String> unitSet)
			throws IOException {

		File cacheFile = new File(TVDB_DIR, "填表單位列表.txt");

		if (!cacheFile.exists()) {
			// Classify tables to organization units
			driver.get("http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp");
			driver.findElement(By.linkText("技專校院")).click();
			driver.findElement(By.linkText("技專校院校務基本資料庫")).click();
			driver.findElement(By.linkText("技專校院")).click();

			(new WebDriverWait(driver, 10)).until(ExpectedConditions
					.elementToBeClickable(By.partialLinkText("輸入表冊一覽表")));
			driver.findElement(By.partialLinkText("輸入表冊一覽表")).click();
			(new WebDriverWait(driver, 30)).until(ExpectedConditions
					.presenceOfElementLocated(By.tagName("table")));

			// Read tables filler table and construct the mapping
			List<WebElement> trs = driver.findElement(By.tagName("table"))
					.findElements(By.tagName("tr"));
			trs.remove(0); // remove table header

			Writer wr = new BufferedWriter(new FileWriter(cacheFile));

			for (WebElement tr : trs) {
				List<WebElement> tds = tr.findElements(By.tagName("td"));
				String tableName = tds.get(1).getText();
				String unitStr = tds.get(5).getText();
				wr.write(tableName + ":" + unitStr + "\n");
			}

			wr.close();
		}

		BufferedReader rd = new BufferedReader(new FileReader(cacheFile));
		String line;

		while ((line = rd.readLine()) != null) {
			String tableName = line.substring(0, line.indexOf(':'));
			String unitStr = line.substring(line.indexOf(':') + 1);

			List<String> units;

			if (!tableUnits.containsKey(tableName)) {
				tableUnits.put(tableName, new ArrayList<String>());
			}

			units = tableUnits.get(tableName);

			for (String unit : unitStr.split(",")) {
				unit = unit.trim();
				if (unit.isEmpty())
					continue;

				units.add(unit);
			}

			unitSet.addAll(units);
		}
	}

	static void obtain總量管制表單位對應表(Map<String, List<String>> tableUnits, Set<String> unitSet)
			throws IOException {

		BufferedReader rd = new BufferedReader(new FileReader(new File(TVDB_WORK_DIR, "總量管制表單位對應表.txt")));
		String line;

		while ((line = rd.readLine()) != null) {
			line = line.trim();
			if(line.isEmpty())
				continue;
			
			String tableName = line.substring(0, line.indexOf(':'));
			String unitStr = line.substring(line.indexOf(':') + 1);

			List<String> units;

			if (!tableUnits.containsKey(tableName)) {
				tableUnits.put(tableName, new ArrayList<String>());
			}

			units = tableUnits.get(tableName);

			for (String unit : unitStr.split(",")) {
				unit = unit.trim();
				if (unit.isEmpty())
					continue;

				units.add(unit);
			}

			unitSet.addAll(units);
		}
	}
}
