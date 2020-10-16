package tvdb;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BasicDataDownloader {

    private static final File output_dir = Utils.TVDB_DIR;
    private static final File download_dir = new File(output_dir, "temp");

    /**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		new File(output_dir, "單位").mkdirs();
		download_dir.mkdirs();
		
		FirefoxProfile profile = Utils.createFireFoxProfile();
		profile.setPreference("browser.download.dir", download_dir.getPath());

		WebDriver driver = new FirefoxDriver(profile);
		
		Utils.openTvdb(driver, "基本表冊");
		
        driver.findElement(By.partialLinkText("列 印 系 統")).click();
        driver.findElement(By.partialLinkText("基本資料庫報表")).click();
        
        (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
        downloadTables(driver, "基本資料表", EnumSet.allOf(OutputType.class));
        
        SortedMap<String, List<String>> tableUnits = new TreeMap<>();
        Set<String> unitSet = new HashSet<String>();
        Utils.obtainTableUnitMapping(tableUnits, unitSet);
        
        Map<String, List<String>> reportUnits = new HashMap<>();
        Utils.obtain報表單位對應表(reportUnits, unitSet);
        
        for(String unit: unitSet) {
    		new File(output_dir, "單位/"+unit+"/表冊資料").mkdirs();
    		new File(output_dir, "單位/"+unit+"/無資料表冊").mkdirs();
        }
        
		// Prepare a set to figure out whether there are files not owned by any unit
		Set<File> orphanFiles = new TreeSet<>();
		for(OutputType type: EnumSet.allOf(OutputType.class)) {
        	    File typeFolder = new File(output_dir, "基本資料表/"+type.name);
            	File[] tableFiles = typeFolder.listFiles(
            	        (FileFilter)new WildcardFileFilter("*"+type.ext));
				orphanFiles.addAll(Arrays.asList(tableFiles));
		}
		
        // Copy table files to the unit in charge according to the table-unit mapping
        for(String tableName: tableUnits.keySet()) {
        	if(tableUnits.get(tableName).isEmpty()) {
        		System.err.println("["+tableName+"]: no unit in charge");
        		continue;
        	}
			
			boolean hasData = false;
        	
        	for(OutputType type: EnumSet.allOf(OutputType.class)) {
        	    File typeFolder = new File(output_dir, "基本資料表/"+type.name);
            	File[] tableFiles = typeFolder.listFiles(
            	        (FileFilter)new WildcardFileFilter(tableName+"(*)"+type.ext));
            	
				if(tableFiles.length != 0) {
					hasData = true;
				}
            	
				orphanFiles.removeAll(Arrays.asList(tableFiles));
				
            	// Maintain a unit-specific list of no-data files per unit
            	for(String unit: tableUnits.get(tableName)) {
            		File unitDir = new File(output_dir, "單位/"+unit);
            		
                	// Copy basic data tables
                	if(tableFiles.length == 1) {
                	    // don't keep the number suffix if there is only one file for current table
                        try {
                            File targetFile = new File(unitDir, "表冊資料/"+tableName+type.ext);
                            if(!targetFile.exists()) {
                                FileUtils.copyFile(tableFiles[0], targetFile);
                            }
                        } catch (IOException e) {
                            System.err.println("Error: ["+tableFiles[0].getName()+"] => ["+unit+"]");
                        }
                	}
                	else {
                        for(File dataFile: tableFiles) {
                            try {
                                File targetFile = new File(unitDir, "表冊資料/"+dataFile.getName());
                                
                                if(!targetFile.exists()) {
                                    FileUtils.copyFile(dataFile, targetFile);
                                }
                                System.out.println("["+dataFile.getName()+"] => ["+unit+"]");
                            } catch (IOException e) {
                                System.err.println("Error: ["+dataFile.getName()+"] => ["+unit+"]");
                            }
                        }
                	}
            	}
        	}
			
			// Create no-data files
			if(!hasData) {
				// Maintain a global list of no-data files
				Path noData = Paths.get(output_dir.getPath(), "基本資料表", tableName+"-無資料.txt");
				try {
					Files.write(noData, 
						tableUnits.get(tableName).stream().collect(joining("\n")).getBytes(StandardCharsets.UTF_8), 
						StandardOpenOption.CREATE);
				}
				catch(IOException e) {
					System.err.println("Error creating no-data file for ["+tableName+"]");
				}
				
				// Create no-data files for all relevent units
            	for(String unit: tableUnits.get(tableName)) {
					noData = Paths.get(output_dir.getPath(), "單位", unit, "無資料表冊", tableName+".txt");
					System.out.println("["+tableName+"-無資料] => ["+unit+"]");
					
					try {
						Files.write(noData, ("表 "+tableName+" 沒有填報資料。\n如果確實無須填報，請寄信告知資訊組。").getBytes(StandardCharsets.UTF_8));
					} catch (IOException e) {
						System.err.println("No-data: Error creating file for ["+tableName+"] ("+unit+")");
					}
				}
			}
        }
        
        // Copy Report to unit folders
        for(OutputType type: EnumSet.allOf(OutputType.class)) {
            File srcDir = new File(output_dir, "基本資料表/"+type.name+"/");
            
            reportUnits.entrySet().stream().forEach(entry -> { 
				String reportName = entry.getKey();
				File[] srcFiles = srcDir.listFiles(
						(FileFilter)new WildcardFileFilter("report"+reportName+"(*)"+type.ext));
				
				orphanFiles.removeAll(Arrays.asList(srcFiles));
						
                entry.getValue().stream().forEach(unit -> {
                    
                    File unitDir = new File(output_dir, "單位/"+unit);
                    
                    if(srcFiles.length == 1) {
						// If only one file exists for the report name, get rid of the trailing (n)
                        try {
                            FileUtils.copyFile(srcFiles[0], new File(unitDir, "report"+reportName+type.ext));
                        } catch (Exception e) {
                            System.err.println("Error: ["+srcFiles[0].getName()+"] => ["+unit+"]: "+e);
                        }
                    }
                    else {
                        Arrays.stream(srcFiles).forEach(file -> {
                            try {
                                FileUtils.copyFile(file, new File(unitDir, file.getName()));
                            } catch (Exception e) {
                                System.err.println("Error: ["+file.getName()+"] => ["+unit+"]: "+e);
                            }
                        });
                    }
                });
            });
        }
		
		if(!orphanFiles.isEmpty()) {
			System.err.println("Orphan Files found:");
			orphanFiles.stream().forEach(f -> System.err.println("  "+f.getName()));
		}

        ((JavascriptExecutor)driver).executeScript("alert('Done!')");
	}

    private static void downloadTables(WebDriver driver, String folder, EnumSet<OutputType> types)
            throws InterruptedException {
        // Begin download

        String mainWin = driver.getWindowHandle();
        String newWin = null;

        // The map to record found table links and the counts.
        // Its keys are table names, its value being the count of a same table
        // name,
        // in case some tables get divided into multiple links with the same
        // link text.
        Map<String, Integer> foundTables = new HashMap<>();

        for (OutputType type : types) {
            new File(output_dir, folder + "/" + type.name).mkdirs();
        }

        Pattern patReport = Pattern.compile("^(report\\d+(_|-)\\d+((_|-)\\d+)*).*$");
        Pattern patTable = Pattern.compile(".*(table\\d+(_|-)\\d+((_|-)\\d+)*).*$");

        // Click each link if not downloaded yet
        List<WebElement> tables = driver.findElements(By.tagName("a"));
        for (WebElement link : tables) {
            String linkText = link.getText();
            Matcher matcher;
            if ((matcher = patReport.matcher(linkText)).matches() || (matcher = patTable.matcher(linkText)).matches()) {
                linkText = matcher.group(1);
            } else {
                System.out.println("Ignored link: " + linkText);
                continue;
            }

            String finalName = linkText.replace('_', '-').replaceFirst(".*table", "");

            int currentTableNumber;

            if (!foundTables.containsKey(finalName)) {
                currentTableNumber = 1;
            } else {
                currentTableNumber = foundTables.get(finalName) + 1;
            }

            foundTables.put(finalName, Integer.valueOf(currentTableNumber));
            finalName = finalName + "(" + currentTableNumber + ")";

            boolean pageLoaded = false;

            for (OutputType type : types) {

                File finalOutput = new File(output_dir, folder + "/" + type.name + "/" + finalName + type.ext);

                if (finalOutput.exists())
                    continue;

                if (!pageLoaded) {
                    link.click();
                    for (String hWnd : driver.getWindowHandles()) {
                        if (!hWnd.equals(mainWin)) {
                            newWin = hWnd;
                            break;
                        }
                    }
                    driver.switchTo().window(newWin);
                    (new WebDriverWait(driver, 60))
                            .until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
                    ((JavascriptExecutor) driver)
                            .executeScript("var imgs = document.getElementsByTagName('img'); " + "var len=imgs.length; "
                                    + "for(var i=len-1; i>=0; i--) {imgs[i].parentNode.removeChild(imgs[i]);} ");

                    pageLoaded = true;
                }

                type.download(driver, finalOutput);

            }
            if (pageLoaded) {
                driver.close();
                driver.switchTo().window(mainWin);
            }
            // break;
        }
    }

    private static enum OutputType {
        EXCEL("Excel", ".xls", (driver) -> {
            // driver.findElement(By.partialLinkText("匯出Excel檔")).click();
            // With the above method, sometimes it hangs waiting for the
            // response after clicking, while the file has already been saved
            ((JavascriptExecutor) driver)
                    .executeScript(driver.findElement(By.partialLinkText("匯出Excel檔")).getAttribute("href"));
        } , download_dir),

        PDF("PDF", ".pdf", (driver) -> {
            ((JavascriptExecutor) driver).executeScript("window.print();");
        } , Utils.BULLZIP_DIR);

        interface TypeDownloader {
            void download(WebDriver driver);
        }

        public final String name;
        public final String ext;
        private TypeDownloader downloader;
        private File generatedFileFolder;

        OutputType(String name, String ext, TypeDownloader downloader, File genFileFolder) {
            this.name = name;
            this.ext = ext;
            this.downloader = downloader;
            this.generatedFileFolder = genFileFolder;

            generatedFileFolder.mkdirs();

            for (File f : generatedFileFolder.listFiles()) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }

        void download(WebDriver driver, File output) {
            downloader.download(driver);
            File newFile = Utils.waitForGeneratedFile(generatedFileFolder);

            if (!newFile.renameTo(output)) {
                System.out.println("Failed to rename [" + newFile.getName() + "] to " + output.getPath());
                newFile.renameTo(new File(output.getParentFile(), newFile.getName()));
            }
        }
    };
}
