/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cms.builder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Manu
 */
public class CMSBuilder {

    private static boolean isFinished = false;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SAXException, IOException, FileNotFoundException, ParserConfigurationException {
        // TODO code application logic here
        final PanelGraficoController pgc = new PanelGraficoController();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    pgc.processAction("cerrarSistema", null);
                } catch (IOException ex) {
                    Logger.getLogger(CMSBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    
}
