package tvdb;

import static java.util.stream.Collectors.toSet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.internet.InternetAddress;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Utils {

	static final File BULLZIP_DIR = new File("D:/temp/Bullzip");
	static final File TVDB_DIR = new File("D:/work/00_tvdb/download");
	static final File TVDB_WORK_DIR = new File("D:/work/00_tvdb/work");
	static final File ASSESS_DIR = new File("D:/work/評鑑/download");
	static final File COMPRESSED_DIR = new File(TVDB_DIR, "compressed");
    private static final String TVDB_NODATA_REASON_DIR = "nodata-reason";

	public static void main(String[] args) throws IOException {
	    openTvdb(createFireFoxDriver(), null);
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

	static void compressAll(File dir, File outputDir) throws IOException {
		for (File subdir : dir.listFiles()) {
			compress(subdir, new File(outputDir, subdir.getName() + ".zip"));
			if (subdir.isFile())
				continue;
		}
	}

	static void compress(File dir, File outputFile) throws FileNotFoundException, IOException {

		List<File> files = new LinkedList<File>();

		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)),
				Charset.forName("big5"));

		files.addAll(Arrays.asList(dir.listFiles()));

		while (!files.isEmpty()) {
			File file = files.remove(0);

			if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
				files.addAll(Arrays.asList(file.listFiles()));
				continue;
			}

			out.putNextEntry(new ZipEntry(dir.toURI().relativize(file.toURI()).getPath()));

			int c;
			byte[] buf = new byte[1024];
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			while ((c = is.read(buf)) != -1) {
				out.write(buf, 0, c);
			}

			is.close();
		}

		out.close();
	}

	static void openTvdb(WebDriver driver, String linkText) throws IOException {
		// Remove old temp files
		for (File f : BULLZIP_DIR.listFiles()) {
			if (f.isFile()) {
				f.delete();
			}
		}

		Properties loginProps = new Properties();
		loginProps.load(Utils.class.getResourceAsStream("login.properties"));

		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//		Dimension winSize = driver.manage().window().getSize();
//		Dimension newWinSize = new Dimension(1700, winSize.height);
//		driver.manage().window().setSize(newWinSize);

		// Don't know why, but document.write seems to hang
		// ((JavascriptExecutor)driver).executeScript(
		// "document.write('<p>請確認 Bullzip printer 輸出路徑已設為
		// "+output_dir.getPath().replace('\\',
		// '/')+"/&lt;time&gt;.pdf<br>" +
		// "確認後請按以下 link 開始下載表冊:<br>" +
		// "<a
		// href=\"http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp\">評鑑基本資料表</a>');"
		// +
		// "document.close();");

		if (linkText == null) {
			driver.get("https://140.125.243.18/");
		} else {
			((JavascriptExecutor) driver).executeScript("var p = document.createElement('p');"
					+ "p.innerHTML= '請確認  Bullzip printer 輸出路徑已設為 " + BULLZIP_DIR.getPath().replace('\\', '/')
					+ "/&lt;time&gt;.pdf<br>" + "確認後請按以下 link 開始下載表冊:<br>';" + "var a = document.createElement('a');"
					+ "a.setAttribute('href', 'https://140.125.243.18/');"
					+ "a.innerHTML = '" + linkText + "';" + "p.appendChild(a);"
					+ "document.getElementsByTagName('body')[0].appendChild(p);");

			// Pop-ups cause subsequent call to WebDriver throwing exception
			// ((JavascriptExecutor)driver).executeScript(
			// "confirm('請確認 Bullzip printer 輸出路徑已設為
			// ["+output_dir.getPath().replace('\\',
			// '/')+"]<time>.pdf');");
			// ((JavascriptExecutor)driver).executeScript(
			// "document.location.href='http://www.tvedb.yuntech.edu.tw/tvedb/index/index.asp';");

			// Wait for the user clicking the link
//			(new WebDriverWait(driver, 100000000)).until(new ExpectedCondition<Boolean>() {
//
//				public Boolean apply(WebDriver drv) {
//					return drv.getCurrentUrl().equals("http://140.125.243.18");
//				}
//
//			});
		}

//		driver.findElement(By.linkText("技專校院")).click();

		// handle IE SSL certificate error
		if (driver instanceof InternetExplorerDriver && !driver.findElements(By.id("overridelink")).isEmpty()) {
			driver.navigate().to("javascript:document.getElementById('overridelink').click()");
		}
		
		WebElement username;
		
		// wait for VPN login page
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				if (d.getCurrentUrl().contains("/remote/login")) {
					return true;
				} else {
					return false;
				}
			}
		});
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		// input VPN auth info
		username = driver.findElement(By.name("username"));
		username.sendKeys(loginProps.getProperty("tvedb.vpn.login"));
		driver.findElement(By.name("credential")).sendKeys(loginProps.getProperty("tvedb.vpn.password"));
//		username.submit();
		driver.findElement(By.id("login_button")).click();

		String mainWin = driver.getWindowHandle();
		driver.findElement(By.cssSelector("button[title='HTTP/HTTPS: 技專校院校務資料庫']")).click();
		
        for (String hWnd : driver.getWindowHandles()) {
            if (!hWnd.equals(mainWin)) {
                driver.switchTo().window(mainWin);
                driver.close();
                driver.switchTo().window(hWnd);
                break;
            }
        }
		driver.findElement(By.linkText("login")).click();

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
		username.sendKeys(loginProps.getProperty("tvedb.web.login"));
		driver.findElement(By.name("user_pwd")).sendKeys(loginProps.getProperty("tvedb.web.password"));
		username.submit();
	}

	static void obtainTableUnitMapping(Map<String, List<String>> tableUnits, Set<String> unitSet)
			throws IOException {

		doObtainTableUnitMapping(tableUnits, unitSet, "填表單位列表.txt");
	}

	static void obtain非當期TableUnitMapping(Map<String, List<String>> tableUnits, Set<String> unitSet)
			throws IOException {

		doObtainTableUnitMapping(tableUnits, unitSet, "非當期填表單位列表.txt");
	}
	
    static Set<String> obtain應無資料Tables()
            throws IOException {

        Map<String, List<String>> tableUnitMap = new TreeMap<>();
        doObtainTableUnitMapping(tableUnitMap, null, "應無資料表冊列表.txt");
        return tableUnitMap.keySet();
    }
    
    static Path getNoDataReasonDir() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR) - 1911;
        int month = cal.get(Calendar.MONTH) < 7 ? 3 : 10;
        return Paths.get(TVDB_WORK_DIR.getPath(), year + "-" + month, TVDB_NODATA_REASON_DIR);
    }

	private static void doObtainTableUnitMapping(Map<String, List<String>> tableUnits, Set<String> unitSet,
			String filename) throws FileNotFoundException, IOException {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR) - 1911;
		int month = cal.get(Calendar.MONTH) < 7 ? 3 : 10;
		File cacheFile = new File(TVDB_WORK_DIR, year + "-" + month + "/" + filename);

		if (!cacheFile.exists()) {
			throw new FileNotFoundException("File not found: " + cacheFile.getAbsolutePath() + "\n"
					+ "Should open 輸入表冊一覽表.ods and run the macro to generate the missing file.");
		}

		obtainTableUnitMappingFromFile(tableUnits, unitSet, cacheFile);
	}

    private static void obtainTableUnitMappingFromFile(Map<String, List<String>> tableUnits, Set<String> unitSet,
            File inputFile) throws IOException, FileNotFoundException {
        
        String line;

		try (BufferedReader rd = new BufferedReader(new FileReader(inputFile))) {
			while ((line = rd.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                
				int indexOfColon = line.indexOf(':');
				String tableName, unitStr;
				
                if(indexOfColon != -1) {
                    tableName = line.substring(0, indexOfColon);
                    unitStr = line.substring(indexOfColon + 1);
                }
                else {
                    tableName = line;
                    unitStr = "";
                }

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

				if (unitSet != null)
					unitSet.addAll(units);
			}
		}
    }

    static String normalizeCheckerName(String checkerName) {
        return checkerName.replace('/', '／');
    }
    
    static Set<String> obtain不得檢出之檢核表() throws IOException {
        File amountControlMappingFile = new File(TVDB_WORK_DIR, "不得檢出之檢核表.txt");
        Map<String, List<String>> checkerUnit = new HashMap<>();
        obtainTableUnitMappingFromFile(checkerUnit, null, amountControlMappingFile);
        return checkerUnit.keySet().stream().map(Utils::normalizeCheckerName).collect(toSet());
    }

	static void obtain總量管制表單位對應表(Map<String, List<String>> tableUnits, Set<String> unitSet) throws IOException {
	    File amountControlMappingFile = new File(TVDB_WORK_DIR, "總量管制表單位對應表.txt");
	    obtainTableUnitMappingFromFile(tableUnits, unitSet, amountControlMappingFile);
	}

    static void obtain報表單位對應表(Map<String, List<String>> tableUnits, Set<String> unitSet) throws IOException {
        File reportUnitMappingFile = new File(TVDB_WORK_DIR, "報表單位對應表.txt");
        obtainTableUnitMappingFromFile(tableUnits, unitSet, reportUnitMappingFile);
    }

	static void obtain單位email(Map<String, List<InternetAddress>> unitEmails) throws IOException {

		String line;

		try (BufferedReader rd = new BufferedReader(new FileReader(new File(TVDB_WORK_DIR, "聯絡人.txt")))) {
			while ((line = rd.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String unitName = line.substring(0, line.indexOf(':'));
				String emailStr = line.substring(line.indexOf(':') + 1);

				List<InternetAddress> emails;

				if (!unitEmails.containsKey(unitName)) {
					unitEmails.put(unitName, new ArrayList<InternetAddress>());
				}

				emails = unitEmails.get(unitName);

				for (String email : emailStr.split(",")) {
					email = email.trim();
					if (email.isEmpty())
						continue;

					emails.add(new InternetAddress(email + "@mail.ntin.edu.tw", unitName, "big5"));
				}
			}
		}

	}

	static void obtainUnitsOfChecker(String checkerName, Map<String, List<String>> tableUnits, Set<String> result) {

		if (checkerName.contains("特殊專班")) {
			result.addAll(Arrays.asList(new String[] { "教務處註冊組", "夜間部", "教務處課務組", "實習輔導處" }));
			return;
		}

		// Parse checker name to figure out related tables
		Matcher tableFinder = Pattern.compile("(表|table)\\s*(\\d+((_|-)\\d+)*(系列)?)", Pattern.CASE_INSENSITIVE)
				.matcher(checkerName);
		while (tableFinder.find()) {
			String tableName = tableFinder.group(2).replace('_', '-');
			if (tableName.endsWith("系列")) {
				tableName = tableName.replace("系列", "");
				for (Map.Entry<String, List<String>> mapEntry : tableUnits.entrySet()) {
					String table = mapEntry.getKey();
					List<String> units = mapEntry.getValue();

					if (table.equals(tableName) || table.startsWith(tableName + "-")
							|| table.startsWith(tableName + "_")) {
						result.addAll(units);
					}
				}
			} else {
				List<String> unit = tableUnits.get(tableName);
				if (unit == null || unit.isEmpty()) {
					System.err.println("No unit-in-charge: [" + tableName + "] in [" + checkerName + "]");
				} else {
					result.addAll(unit);
				}
			}
		}
	}
	
    static public FirefoxDriver createFireFoxDriver() {
        return createFireFoxDriver(createFireFoxProfile());
    }
    
	static public FirefoxProfile createFireFoxProfile() {
//	    ProfilesIni allProfile = new ProfilesIni();
//	    FirefoxProfile profile = allProfile.getProfile("tvedb");
	    FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.startup.homepage_override.mstone", "ignore");
        profile.setPreference("startup.homepage_welcome_url.additional",  "about:blank");
        profile.setPreference("intl.charset.fallback.override", "Big5");
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
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/vnd.ms-excel,application/vnd.ms-execl");
        
        profile.setPreference("security.ssl3.dhe_rsa_aes_128_sha", false);
        profile.setPreference("security.ssl3.dhe_rsa_aes_256_sha", false);
//      profile.setPreference("webdriver_accept_untrusted_certs", true);
        profile.setAcceptUntrustedCertificates(true);
        profile.setAssumeUntrustedCertificateIssuer(false);
        return profile;
	}
	
    static public FirefoxDriver createFireFoxDriver(FirefoxProfile profile) {
        System.setProperty("webdriver.gecko.driver", "d:/home/tvedb/geckodriver.exe");
		
//        ProfilesIni allProfile = new ProfilesIni();
//        FirefoxProfile profile = allProfile.getProfile("tvedb"); 
//        profile.setPreference("browser.startup.homepage_override.mstone", "ignore");
//        profile.setPreference("startup.homepage_welcome_url.additional",  "about:blank");
        DesiredCapabilities caps = DesiredCapabilities.firefox();
//        caps.setCapability("marionette", true);        
        caps.setCapability(FirefoxDriver.PROFILE, profile);
		
		FirefoxDriver driver = new FirefoxDriver(caps);
		return driver;
	}
}
