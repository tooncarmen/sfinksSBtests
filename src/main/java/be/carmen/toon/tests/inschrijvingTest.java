package be.carmen.toon.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class inschrijvingTest {

    private static final CharSequence PASSWD = "xxx";
    private FirefoxDriver driver;
    private HashMap<String, String> formValues;
    private String tokenKey;


    @Before
    public void setup() {
        System.setProperty("webdriver.gecko.driver", "src\\main\\resources\\geckodriver.exe");
        FirefoxBinary firefoxBinary = new FirefoxBinary();
        //  firefoxBinary.addCommandLineOptions("-headless");
        //firefoxBinary.addCommandLineOptions("-p 'test-suite'");
        firefoxBinary.addCommandLineOptions("-private");
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setLogLevel(FirefoxDriverLogLevel.ERROR);
        firefoxOptions.setHeadless(true);
        firefoxOptions.setBinary(firefoxBinary);
        driver = new FirefoxDriver(firefoxOptions);
        formValues = new HashMap<String, String>();

    }

    @Test
    public void PageAndPageNavigationTest() {

        //Step 2- Navigation: Open a website
        driver.navigate().to("http://sfinks.d-en-v.be/");
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        Assert.assertEquals("header loaded", "Inschrijving Sfeer Beheer Sfinks", driver.getTitle());
        LOG("Homepage OK");
        System.out.println();
        //link to info page
        driver.findElement(By.xpath("/html/body/div[3]/div[2]/p[2]/a")).click();
        Assert.assertEquals("header loaded", "Sfinks Sfeer beheer Info", driver.getTitle());
        LOG("HP -> info page OK");

        //link back to inschr page
        driver.findElement(By.xpath("/html/body/div[4]/p[3]/a")).click();
        Assert.assertEquals("header loaded", "Inschrijving Sfeer Beheer Sfinks", driver.getTitle());
        LOG("info -> HP OK");

        //link to mijnshiften
        driver.findElement(By.xpath("/html/body/div[3]/div[2]/p[5]/a")).click();
        Assert.assertEquals("header loaded", "Mijn Shiften Sfeer beheer Sfinks", driver.getTitle());
        LOG("HP -> mijnshiften OK");

        //link back to inschr page
        driver.findElement(By.xpath("/html/body/div[2]/div/p[2]/a")).click();
        Assert.assertEquals("header loaded", "Inschrijving Sfeer Beheer Sfinks", driver.getTitle());
        LOG("mijnshiften -> HP OK");

    }
    @Ignore
    @Test
    public void InschrijvenTest() throws InterruptedException {
        SetFormValues();
        removeifUserExists();
        Inschrijving();
        // check confirmation

        Assert.assertTrue(driver.getPageSource().contains("Uw gegevens zijn succesvol opgeslagen!"));
        for (String s : formValues.values()) {
            Assert.assertTrue(driver.getPageSource().contains(s));
        }
        // RETRY

        Inschrijving();

        //already in db
        driver.switchTo().alert().accept();
        Assert.assertEquals("header loaded", "Mijn Shiften Sfeer beheer Sfinks", driver.getTitle());
        String url = driver.getCurrentUrl();
        tokenKey = extractTokenFromUrl(url);

        Assert.assertTrue(driver.getPageSource().contains(formValues.get("vnaam") + " " + formValues.get("naam")));
        Assert.assertTrue(driver.getPageSource().contains("Donderdag 1"));
        Assert.assertTrue(driver.getPageSource().contains("Donderdag 2"));
        LOG("*** TOKEN = " + tokenKey);

        //check mijnshiften
        driver.navigate().to("http://sfinks.d-en-v.be/mijnshiften.php");
        Assert.assertEquals("header loaded", "Mijn Shiften Sfeer beheer Sfinks", driver.getTitle());
        driver.findElement(By.xpath("//*[@id=\"email\"]")).sendKeys(formValues.get("email"));
        LocalDate bday = LocalDate.parse(formValues.get("gdat"), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        driver.findElement(By.xpath("//*[@id=\"gjaar\"]")).sendKeys("" + bday.getYear());
        driver.findElement(By.xpath("//*[@id=\"send\"]")).click();

        Assert.assertTrue(driver.getPageSource().contains(formValues.get("vnaam") + " " + formValues.get("naam")));
        Assert.assertTrue(driver.getPageSource().contains("Donderdag 1"));
        Assert.assertTrue(driver.getPageSource().contains("Donderdag 2"));

        removeUser(tokenKey);
    }

    private void removeUser(String tokenKey) {
        // undo
        driver.navigate().to("http://sfinks.d-en-v.be/admin/?killuser=" + tokenKey);
        //login
        driver.findElement(By.xpath("/html/body/form/input[1]")).sendKeys(PASSWD);
        driver.findElement(By.xpath("/html/body/form/input[2]")).click();
        Assert.assertTrue(driver.getPageSource().contains(formValues.get("vnaam") + " " + formValues.get("naam")));
        Assert.assertTrue(driver.getPageSource().contains("!! REMOVING USER !!"));
        Assert.assertTrue(driver.getPageSource().contains("Removing stuard: success"));
        Assert.assertTrue(driver.getPageSource().contains("Removing stuard tewerkstelling: success"));
        //logout
        driver.findElement(By.xpath("//*[@id=\"logout\"]")).click();
        LOG("USER REMOVED with name: " + formValues.get("naam"));
    }

    private void Inschrijving() {
        driver.navigate().to("http://sfinks.d-en-v.be/");
        Assert.assertEquals("header loaded", "Inschrijving Sfeer Beheer Sfinks", driver.getTitle());


        driver.findElement(By.id("toggleButton")).click();
        filloutFormByValues(formValues);

        clickIds("p1", "p2", "l1", "l2", "send");

        // redirect to next page
        Assert.assertEquals("header loaded", "Bevestiging inschrijving", driver.getTitle());

        //all values are there
        for (String s : formValues.values()) {
            Assert.assertTrue(driver.getPageSource().contains(s));
        }
        //periode and periode check?
        Assert.assertTrue(driver.getPageSource().contains("Donderdag middag"));
        Assert.assertTrue(driver.getPageSource().contains("Donderdag avond"));

        //confirm
        clickIds("bevestig");


    }

    private String extractTokenFromUrl(String url) {
        Pattern p = Pattern.compile("\\?token=([a-z0-9]+)");
        Matcher m = p.matcher(url);
        LOG("*** URL = " + url);
        if (!m.find()) {
            LOG("*** TOKEN KEY NOT FOUND");
            return "";
        }
        //Assert.assertTrue("Tokenkey not found in url", m.find());
        return m.group(1);
    }

    private void SetFormValues() {
        formValues.put("naam", "TestDummy");
        formValues.put("vnaam", "Johny");
        formValues.put("rrn", "11.11.11-123.12");
        formValues.put("email", "TestJohn@nomail.com");
        formValues.put("gdat", "12/01/1990");
        formValues.put("gsm", "0485368000");
        formValues.put("straat", "DezeStraat");
        formValues.put("huisnr", "100");
        formValues.put("postcode", "1111");
        formValues.put("gemeente", "ThisTown");
        formValues.put("opm", "TEST - THIS CAN BE REMOVED");
        LOG("Values filled out");
    }

    @After
    public void finish() throws InterruptedException {


        //Step 4- Close Driver
        // driver.close();
        // Thread.sleep(1000);
        //Step 5- Quit Driver
        driver.quit();
    }

    private void filloutFormByValues(HashMap<String, String> values) {
        for (String s : values.keySet()) {
            LOG("SET: " + s + "=" + values.get(s));
            WebElement element = driver.findElement(By.xpath("//*[@id=\"" + s + "\"]"));
            element.sendKeys(values.get(s));
            Assert.assertEquals(values.get(s), element.getAttribute("value"));
        }
    }

    private void clickIds(String... ids) {
        for (String s : ids) {
            LOG("Click: " + s);
            WebElement element = driver.findElement(By.xpath("//*[@id=\"" + s + "\"]"));
            element.click();
        }
    }

    private void removeifUserExists() {
        driver.navigate().to("http://sfinks.d-en-v.be/mijnshiften.php");
        Assert.assertEquals("header loaded", "Mijn Shiften Sfeer beheer Sfinks", driver.getTitle());
        driver.findElement(By.xpath("//*[@id=\"email\"]")).sendKeys(formValues.get("email"));
        LocalDate bday = LocalDate.parse(formValues.get("gdat"), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        driver.findElement(By.xpath("//*[@id=\"gjaar\"]")).sendKeys("" + bday.getYear());
        driver.findElement(By.xpath("//*[@id=\"send\"]")).click();
        Assert.assertEquals("header loaded", "Mijn Shiften Sfeer beheer Sfinks", driver.getTitle());
        String url = driver.getCurrentUrl();
        tokenKey = extractTokenFromUrl(url);
        if (!tokenKey.equals("")) {
            removeUser(tokenKey);
        }
    }

    private static void LOG(String s) {
        LocalDateTime date = LocalDateTime.now();
        System.out.printf("%-10s | %s \n", date.format(DateTimeFormatter.ISO_TIME), s);
    }
}
