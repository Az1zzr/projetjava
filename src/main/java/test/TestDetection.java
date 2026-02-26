package test;
import utils.SensitiveImageDetector;

public class TestDetection {
    public static void main(String[] args) throws Exception {
        String cheminImage = "test.jpg"; // mets une vraie image
        SensitiveImageDetector.Result result = SensitiveImageDetector.analyze(cheminImage);
        System.out.println("Niveau  : " + result.getLevel());
        System.out.println("Sensible: " + result.isSensitive());
        System.out.println("Raison  : " + result.getReason());
    }
}
