/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package cms.builder;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Manu
 */
public class PanelGraficoController {
    
    private static String configFile = "config";
    private Document m_Document = null;
    private PanelGrafico m_PanelContainer = null;
    private JFrame m_ConfigFrame = null;
    
    //DB
    Connection m_Connection = null;
    Statement m_Statement = null;
    ResultSet m_Last_ResultSet = null;
    //FIN DB
    
    // ARCHIVO DE AUTOMATIZACIÓN DESGUAZADO EN LÍNEAS
    String[][] m_Automatization_File = null;
    // Cabecera del archivo de automatización
    Map<String,Integer> m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion = new HashMap<String,Integer>();
    
    public PanelGraficoController() throws SAXException, IOException, FileNotFoundException, ParserConfigurationException {
        m_PanelContainer = new PanelGrafico(this);
        m_ConfigFrame = new JFrame();
        m_ConfigFrame.add(m_PanelContainer);
        
        readConfig();
        
        /* OLD CODE
        NodeList configNodeList = m_Document.getElementsByTagName("config"); // nodo config
        Element configElement = (Element) configNodeList.item(0); // solo hay un elemento
        NodeList sustituciones_NodeList = configElement.getElementsByTagName("sustituciones");
        Element sustitucionesElement = (Element) sustituciones_NodeList.item(0); // solo hay un elemento
        */
        
        // load_sustituciones_combobox();
        
        load_text_fields();
        
        m_PanelContainer.bd_jPanel.setSize(m_PanelContainer.bd_jPanel.getPreferredSize());
        m_PanelContainer.archivos_jPanel.setSize(m_PanelContainer.archivos_jPanel.getPreferredSize());
        m_PanelContainer.sustituciones_jPanel.setSize(m_PanelContainer.sustituciones_jPanel.getPreferredSize());
        m_PanelContainer.procesarCMSjPanel.setSize(m_PanelContainer.procesarCMSjPanel.getPreferredSize());
        m_PanelContainer.cms_por_lotes_jPanel.setSize(m_PanelContainer.cms_por_lotes_jPanel.getPreferredSize());
        
        
        m_ConfigFrame.setVisible(true);
        m_ConfigFrame.pack();
    }
    
    private void readConfig() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        /*
        * Con motivos de depuración imprimimos el current working directory
        */
        System.out.println("Working Directory = "
                + System.getProperty("user.dir"));
        
        FileInputStream istream = new FileInputStream(configFile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        
        /* Disable validation, we need to speed up */
        //dbFactory.setNamespaceAware(false);
        //dbFactory.setValidating(false);
        
        //dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
        //dbFactory.setFeature("http://xml.org/sax/features/validation", false);
        // dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        // dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        m_Document = dBuilder.parse(istream);
        
        //optional, but recommended
        //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        m_Document.getDocumentElement().normalize();
    }
    
    void saveConfig() throws TransformerConfigurationException, TransformerException {
        
        ArrayList<Node> text_nodes = extractTextChildrenNodes(m_Document.getDocumentElement());
        this.removePassedNodes(text_nodes);
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
        Result output = new StreamResult(new File(configFile));
        Source input = new DOMSource(m_Document);
        
        transformer.transform(input, output);
    }
    
    // acceso por defecto: al mismo paquete
    Object processAction(String accion, Object... args) throws FileNotFoundException, IOException{
        Object result = null;
        if ((accion != null) && (!accion.equals(""))){ // ni es null ni cadena vacía
            
            String texto_busqueda = null;
            JTextField sustituciones_search_string_text_field = null;
            NodeList sustituciones_NodeList = null;
            Element sustitucionesElement = null; // solo hay un elemento
            Element element_DOM = null;
            //String meta_titulo = null;
            //String meta_palabras_clave = null;
            //String meta_descripcion = null;
            //String url_amigable = null;
            int int_arg;
            Object arg_obj;
            Map<String, String> resultado_proceso = null;
            ResultSet max_position_resultSet = null;
            int max_position = 0;
            
            switch(accion){
                /*
                case "guardarNuevaSustitucion":
                    texto_busqueda = m_PanelContainer.nuevoNombreSustitucionTextField.getText();
                    
                    if (!existeSustitucion(texto_busqueda)){
                        String sustitucion = m_PanelContainer.nuevoValorSustitucionTextField.getText();
                        
                        sustituciones_NodeList = m_Document.getElementsByTagName("sustituciones");
                        sustitucionesElement = (Element) sustituciones_NodeList.item(0); // solo hay un elemento
                        
                        
                        Element new_element = m_Document.createElement("nodo_sustitucion");
                        new_element.setAttribute("buscar", texto_busqueda);
                        new_element.setAttribute("sustituir", sustitucion);
                        
                        sustitucionesElement.appendChild(new_element);
                        sustituciones_search_string_text_field = this.m_PanelContainer.sustitucionesComboBox;
                        sustituciones_search_string_text_field.removeAllItems();
                        
                        load_sustituciones_combobox();
                        
                    }
                    break;
                */    
                case "cambiarSustitucion":
                    arg_obj = args[0];
                    int_arg = (Integer)arg_obj;
                    sustituciones_search_string_text_field = this.m_PanelContainer.m_Indexed_Sustitution_Seach_String_JtextField_Map.get(int_arg);
                    
                    texto_busqueda = (String) sustituciones_search_string_text_field.getText();
                    
                    String new_valor = m_PanelContainer.m_Indexed_Sustitution_JtextField_Map.get(int_arg).getText();
                    
                    
                    
                    Element element_to_change_value = getElementSustitucion(texto_busqueda);
                    
                    if (element_to_change_value != null){
                        element_to_change_value.setAttribute("sustituir", new_valor);
                    }
                    else {
                        Element new_element = m_Document.createElement("nodo_sustitucion");
                        new_element.setAttribute("buscar", texto_busqueda);
                        new_element.setAttribute("sustituir", new_valor);
                        
                        sustituciones_NodeList = m_Document.getElementsByTagName("sustituciones");
                        sustitucionesElement = (Element) sustituciones_NodeList.item(0); // solo hay un elemento
                        
                        sustitucionesElement.appendChild(new_element);
                    }
                    
                    break;
                    
                case "borrarSustitucion":
                    
                    int_arg = (int)args[0];
                    
                    sustituciones_search_string_text_field = this.m_PanelContainer.m_Indexed_Sustitution_Seach_String_JtextField_Map.get(int_arg);
                    
                    texto_busqueda = (String) sustituciones_search_string_text_field.getText();
                    
                    Element element_to_remove = getElementSustitucion(texto_busqueda);
                    
                    sustituciones_NodeList = m_Document.getElementsByTagName("sustituciones");
                    sustitucionesElement = (Element) sustituciones_NodeList.item(0); // solo hay un elemento
                    
                    sustitucionesElement.removeChild(element_to_remove);
                    
                    sustituciones_search_string_text_field.setText("");
                    m_PanelContainer.m_Indexed_Sustitution_JtextField_Map.get(int_arg).setText("");

                    break;
                    
                case "cambiarMillares":
                    String nuevos_millares = m_PanelContainer.archivoMillaresTextField.getText();
                    element_DOM = getElementArchivos("millares");
                    element_DOM.setAttribute("nombre", nuevos_millares);
                    
                    break;
                    
                case "cambiarCentenas":
                    String nuevas_centenas = m_PanelContainer.archivoCentenasTextField.getText();
                    element_DOM = getElementArchivos("centenas");
                    element_DOM.setAttribute("nombre", nuevas_centenas);
                    
                    break;
                    
                case "cambiarDecenas":
                    String nuevas_decenas = m_PanelContainer.archivoDecenasTextField.getText();
                    element_DOM = getElementArchivos("decenas");
                    element_DOM.setAttribute("nombre", nuevas_decenas);
                    
                    break;
                    
                case "cambiarUnidades":
                    String nuevas_unidades = m_PanelContainer.archivoUnidadesTextField.getText();
                    element_DOM = getElementArchivos("unidades");
                    element_DOM.setAttribute("nombre", nuevas_unidades);
                    
                    break;
                    
                case "cambiarProvincias":
                    String nuevas_provincias = m_PanelContainer.archivoProvinciasTextField.getText();
                    element_DOM = getElementArchivos("provincias");
                    element_DOM.setAttribute("nombre", nuevas_provincias);
                    
                    break;
                    
                case "cambiarCiudades":
                    String nuevas_ciudades = m_PanelContainer.archivoCiudadesTextField.getText();
                    element_DOM = getElementArchivos("ciudades");
                    element_DOM.setAttribute("nombre", nuevas_ciudades);
                    
                    break;
                    
                case "cambiarMetatitulo":
                    String nuevo_metatitulo = m_PanelContainer.archivoMetatituloTextField.getText();
                    element_DOM = getElementArchivos("titulo");
                    element_DOM.setAttribute("nombre", nuevo_metatitulo);
                    
                    break;
                    
                case "cambiarMetadescripcion":
                    String nuevo_metadescripcion = m_PanelContainer.archivoMetadescripcionTextField.getText();
                    element_DOM = getElementArchivos("descripcion");
                    element_DOM.setAttribute("nombre", nuevo_metadescripcion);
                    
                    break;
                    
                case "cambiarMetapalabrasclave":
                    String nuevo_metapalabrasclave = m_PanelContainer.archivoMetapalabrasclaveTextField.getText();
                    element_DOM = getElementArchivos("palabras_clave");
                    element_DOM.setAttribute("nombre", nuevo_metapalabrasclave);
                    
                    break;
                    
                case "cambiarURL":
                    String nueva_url_amigable = m_PanelContainer.archivoUrlamigableTextField.getText();
                    element_DOM = getElementArchivos("url");
                    element_DOM.setAttribute("nombre", nueva_url_amigable);
                    
                    break;
                    
                case "cambiarUriDB":
                    String nueva_uri = m_PanelContainer.uriDB_TextField.getText();
                    element_DOM = getElementDB("uri");
                    element_DOM.setAttribute("valor", nueva_uri);
                    
                    break;
                    
                case "cambiarUsuarioDB":
                    String nuevo_user = m_PanelContainer.usuarioDB_TextField.getText();
                    element_DOM = getElementDB("usuario");
                    element_DOM.setAttribute("valor", nuevo_user);
                    
                    break;
                    
                case "cambiarContrasena":
                    String nueva_contrasena = m_PanelContainer.contrasenaDB_TextField.getText();
                    element_DOM = getElementDB("contrasena");
                    element_DOM.setAttribute("valor", nueva_contrasena);
                    
                    break;
                    
                case "procesar_y_guardar_cms":
                    resultado_proceso = procesarCMS_completo(null, null);
                    
                    String nombre_archivo_guardar = m_PanelContainer.nombreArchivoGuardarCMS_TextField.getText();

                    boolean existeCarpeta = existFolder("resultado_archivos");
                    if (!existeCarpeta){
                        createFolder("resultado_archivos");
                    }
                    
                    int extension_index = nombre_archivo_guardar.lastIndexOf(".");
                    
                    String string_sin_extension = null;
                    String extension = null;
                    
                    if (extension_index != -1){
                        string_sin_extension = nombre_archivo_guardar.substring(0, extension_index);
                        extension = nombre_archivo_guardar.substring(extension_index);
                    }
                    else {
                        string_sin_extension = nombre_archivo_guardar;
                        extension = ".html"; // SI NO TIENE EXTENSIÓN SE LA AÑADIMOS
                    }
                    
                    /*
                    result.put("cms", codigo_cms);
                    result.put("titulo", meta_titulo);
                    result.put("palabras_clave", meta_palabras_clave);
                    result.put("descripcion", meta_descripcion);
                    result.put("url", url_amigable);
                    */
                    
                    this.save_To_File("resultado_archivos/"+nombre_archivo_guardar+extension, resultado_proceso.get("cms"));
                    save_To_File("resultado_archivos/"+string_sin_extension+"_meta_titulo"+extension, resultado_proceso.get("titulo"));
                    save_To_File("resultado_archivos/"+string_sin_extension+"_meta_palabras_clave"+extension, resultado_proceso.get("palabras_clave"));
                    save_To_File("resultado_archivos/"+string_sin_extension+"_meta_descripcion"+extension, resultado_proceso.get("descripcion"));
                    save_To_File("resultado_archivos/"+string_sin_extension+"_url_amigable"+extension, resultado_proceso.get("url"));
                    
                    break;
                
                case "procesar_y_subir_cms":
                    
                    String mensajeOK = null;
                    
                    /* Parametros que se pueden pasar a processAction para esta accion:
                     * args[0] idCMS
                     * args[1] idIdioma
                     * args[2] idConstruccionCMS
                     * args[3] estructura de sustituciones
                     * args[4] idCategory
                     *
                     * Si algún parámetro no se pasa se entiende que el procesamiento es
                     * de los cms individuales y se cogerá el textField correspondiente
                     * a la creación de dicho CMS
                     *
                     */
                    
                    String idCMS = (args.length>0) && args[0]!=null?(String)args[0]:this.m_PanelContainer.idCMSPrestashop_TextField.getText();
                    String idIdioma = (args.length>1) && args[1]!=null?(String)args[1]:this.m_PanelContainer.idiomaCMS_TextField.getText();
                    String idCategory = (args.length>5) && args[5]!=null?(String)args[5]:this.m_PanelContainer.idiomaCMS_TextField.getText();
                    String id_tienda = (args.length>6) && args[6]!=null?(String)args[6]:this.m_PanelContainer.id_tienda_TextField.getText();
                    
                    resultado_proceso = procesarCMS_completo((args.length>2)?(String)args[2]:null, (args.length>3)?(String[][])args[3]:null);

                    boolean mostrar_mensaje_emergente_finalizacion = (args.length>4) ? (boolean)args[4]:true;
                    
                    if (idCMS.equals("") || idIdioma.equals("")) {
                        JOptionPane.showMessageDialog(this.m_ConfigFrame, "Si desea subir el cms rellene los campos de id de cms e id de idioma");
                    }
                    else {
                        try {
                            connectMysqlDB();
                            /*
                            String query_consulta_prueba = "SELECT * FROM "+getElementDB("db_prefix").getAttribute("valor")
                            +"cms_lang WHERE id_cms = "+m_PanelContainer.idCMSPrestashop_TextField.getText()
                            +" AND id_lang = "+m_PanelContainer.idiomaCMS_TextField.getText();
                            */
                            // EJEMPLO: INSERT INTO Students (StudentId, name) VALUES ('123', 'Jones');
                            String insert_SQL = "INSERT INTO "+getElementDB("db_prefix").getAttribute("valor")
                                    +"cms_lang (meta_title, meta_description, meta_keywords, content, link_rewrite, id_cms, id_lang) "
                                    + "VALUES (?, ?, ?, ?, ?, "
                                    + idCMS+", "
                                    + idIdioma+")"
                                    + "ON DUPLICATE KEY "
                                    + "UPDATE meta_title=?, meta_description=?, "
                                    + "meta_keywords=?, content=?, link_rewrite=?";
                            
                            PreparedStatement statement = m_Connection.prepareStatement(insert_SQL);
                            
                            statement.setString(1, resultado_proceso.get("titulo"));
                            statement.setString(2, resultado_proceso.get("descripcion"));
                            statement.setString(3, resultado_proceso.get("palabras_clave"));
                            statement.setString(4, resultado_proceso.get("cms"));
                            statement.setString(5, resultado_proceso.get("url"));
                            
                            statement.setString(6, resultado_proceso.get("titulo"));
                            statement.setString(7, resultado_proceso.get("descripcion"));
                            statement.setString(8, resultado_proceso.get("palabras_clave"));
                            statement.setString(9, resultado_proceso.get("cms"));
                            statement.setString(10, resultado_proceso.get("url"));
                            
                            try {
                                //this.executeQuery(query_consulta_prueba);
                                int filas_afectadas = statement.executeUpdate();
                                int filas_afectadas_2;
                                if (filas_afectadas == 0){ // ERROR
                                    JOptionPane.showMessageDialog(this.m_ConfigFrame, "Un error ha impedido subir el cms a la tabla CMS_LANG");
                                }
                                else {
                                    String consulta_sql = "SELECT COUNT(*) AS COUNT, id_cms FROM "+getElementDB("db_prefix").getAttribute("valor")+"cms "
                                            + "WHERE id_cms = "+idCMS;
                                    this.executeQuery(consulta_sql);
                                    int result_size = 0;
                                    while(this.m_Last_ResultSet.next()) {
                                        result_size = m_Last_ResultSet.getInt("COUNT");
                                        break;
                                    }
                                    if (result_size == 0){ // EL CMS NO EXISTE, AÑADIR
                                        // INSERTAR EN TABLA CMS
                                        m_Connection.setAutoCommit(false);
                                        
                                        String max_current_position_sql = "SELECT MAX(position)+1 AS MAX_POSITION FROM "+getElementDB("db_prefix").getAttribute("valor")+"cms";
                                        statement = m_Connection.prepareStatement(max_current_position_sql);
                                        max_position_resultSet = statement.executeQuery();
                                        if (max_position_resultSet.next()){ // resultado encontrado
                                            max_position = max_position_resultSet.getInt("MAX_POSITION");
                                        }
                                        
                                        
                                        insert_SQL = "INSERT INTO "+getElementDB("db_prefix").getAttribute("valor")
                                                +"cms(active, id_cms, id_cms_category, position) "
                                                + "VALUES ('1','"
                                                + idCMS +"','"+ idCategory + "','" + (max_position+1) + "')"
                                                + "ON DUPLICATE KEY "
                                                + "UPDATE active=1,"// id_cms="+idCMS+", "
                                                + "id_cms_category="+idCategory+", position="+ (max_position+1);
                                        statement = m_Connection.prepareStatement(insert_SQL);
                                        filas_afectadas = statement.executeUpdate();
                                        m_Connection.commit();
                                        
                                        m_Connection.setAutoCommit(true);
                                        
                                        insert_SQL = "INSERT INTO "+getElementDB("db_prefix").getAttribute("valor")
                                                +"cms_shop(id_cms, id_shop) "
                                                + "VALUES ('"+idCMS+"','"
                                                + id_tienda + "')"
                                                + "ON DUPLICATE KEY "
                                                + "UPDATE id_cms="+idCMS+", id_shop="+id_tienda;
                                        statement = m_Connection.prepareStatement(insert_SQL);
                                        filas_afectadas_2 = statement.executeUpdate();
                                        
                                        if ((filas_afectadas == 1) && (filas_afectadas_2 == 1)){ // INSERTADO CORRECTAMENTE
                                            mensajeOK = "El CMS con ID "+ idCMS+ " se ha INSERTADO con éxito";
                                            if (mostrar_mensaje_emergente_finalizacion == true){
                                                JOptionPane.showMessageDialog(this.m_ConfigFrame, mensajeOK);
                                            }
                                            result = new String[]{"INSERTADO", mensajeOK};
                                        }
                                        else {
                                            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Un error ha impedido subir el cms a la tabla CMS");
                                        }
                                    }
                                    else {
                                        mensajeOK = "El CMS con ID "+ idCMS+ " se ha ACTUALIZADO con éxito";
                                        if (mostrar_mensaje_emergente_finalizacion == true){
                                            JOptionPane.showMessageDialog(this.m_ConfigFrame, mensajeOK);
                                        }
                                        result = new String[]{"ACTUALIZADO", mensajeOK};
                                    }
                                }                
                            }
                            catch (SQLException e){
                                JOptionPane.showMessageDialog(this.m_ConfigFrame, "Un error ha impedido subir la información a la base de datos");
                            }
                            /*
                            while(this.m_Last_ResultSet.next()){
                            String prueba = m_Last_ResultSet.getString("content");
                            System.out.println("resultado de la consulta a base de datos = "+prueba);
                            }
                            */
                            
                        }
                        catch(SQLException e){
                            JOptionPane.showMessageDialog(this.m_ConfigFrame, "No se ha podido conectar a la base de datos, revise los datos de conexión");
                            System.err.println(e.getMessage());
                            e.printStackTrace();
                        }
                        
                    }
                    break;
                    
                case "cambiarDB_Name":
                    String db_name = m_PanelContainer.dB_name_TextField.getText();
                    element_DOM = getElementDB("db_name");
                    element_DOM.setAttribute("valor", db_name);
                    break;
                    
                case "cambiarPrefijoDB":
                    String db_prefix = m_PanelContainer.table_prefix_TextField.getText();
                    element_DOM = getElementDB("db_prefix");
                    element_DOM.setAttribute("valor", db_prefix);
                    break;
                    
                case "cambiarArchivoAutomatizacionLotes":
                    
                    String nuevo_archivo_automatizacion = m_PanelContainer.archivoAutomatizacionLotesTextField.getText();
                    element_DOM = getElementArchivos("automatizacion");
                    element_DOM.setAttribute("nombre", nuevo_archivo_automatizacion);
                    break;
                
                case "cargarArchivoLotes":
                    this.leer_archivo_automatizacion_por_lotes();
                    this.setAutomatizationFileHeader();
                    break;
                    
                case "procesarLote":
                    procesarLote();
                    break;
                    
                case "cerrarSistema":
                    cerrarSistema();
                    break;
            }
        }
        return result;
    }
    
    private boolean existeSustitucion(String texto_busqueda){
        boolean result = false;
        if ((texto_busqueda != null) && (!texto_busqueda.equals(""))){
            NodeList nodo_sustitucion_NodeList = m_Document.getElementsByTagName("nodo_sustitucion");
            int numero_sustituciones = nodo_sustitucion_NodeList.getLength();
            for (int i = 0; i < numero_sustituciones; i++){
                Element elemento_sustitucion = (Element)nodo_sustitucion_NodeList.item(i);
                if (elemento_sustitucion.getAttribute("buscar").equalsIgnoreCase(texto_busqueda)){
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
    
    private Element getElementSustitucion(String texto_busqueda){
        Element result = null;
        if ((texto_busqueda != null) && (!texto_busqueda.equals(""))){
            NodeList nodo_sustitucion_NodeList = m_Document.getElementsByTagName("nodo_sustitucion");
            int numero_sustituciones = nodo_sustitucion_NodeList.getLength();
            for (int i = 0; i < numero_sustituciones; i++){
                Element elemento_sustitucion = (Element)nodo_sustitucion_NodeList.item(i);
                if (elemento_sustitucion.getAttribute("buscar").equalsIgnoreCase(texto_busqueda)){
                    result = elemento_sustitucion;
                    break;
                }
            }
        }
        return result;
    }
    
    /*
    private void load_sustituciones_combobox(){
        // SUSTITUCIONES
        NodeList nodo_sustitucion_NodeList = m_Document.getElementsByTagName("nodo_sustitucion");
        int numero_sustituciones = nodo_sustitucion_NodeList.getLength();
        JComboBox sustitucionesComboBox = this.m_PanelContainer.sustitucionesComboBox;
        
        for (int i = 0; i < numero_sustituciones; i++) {
            sustitucionesComboBox.addItem(((Element) nodo_sustitucion_NodeList.item(i)).getAttribute("buscar"));
        }
    }
    */
    
    private Element getElementArchivos(String uso_archivo){
        Element result = null;
        if ((uso_archivo != null) && (!uso_archivo.equals(""))){
            NodeList nodo_sustitucion_NodeList = m_Document.getElementsByTagName("nodo_archivo");
            int numero_nodos_archivo = nodo_sustitucion_NodeList.getLength();
            for (int i = 0; i < numero_nodos_archivo; i++){
                Element elemento_sustitucion = (Element)nodo_sustitucion_NodeList.item(i);
                if (elemento_sustitucion.getAttribute("uso_archivo").equalsIgnoreCase(uso_archivo)){
                    result = elemento_sustitucion;
                    break;
                }
            }
        }
        return result;
    }
    
    private void load_text_fields(){
        // ARCHIVOS
        NodeList archivos_NodeList = m_Document.getElementsByTagName("nodo_archivo");
        int numero_archivos = archivos_NodeList.getLength();
        
        // Automatización
        JTextField archivoAutomatizacionTextField = this.m_PanelContainer.archivoAutomatizacionLotesTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("automatizacion")){
                    archivoAutomatizacionTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Millares
        JTextField archivoMillaresTextField = this.m_PanelContainer.archivoMillaresTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("millares")){
                    archivoMillaresTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Centenas
        JTextField archivoCentenasTextField = this.m_PanelContainer.archivoCentenasTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("centenas")){
                    archivoCentenasTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Decenas
        JTextField archivoDecenasTextField = this.m_PanelContainer.archivoDecenasTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("decenas")){
                    archivoDecenasTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Unidades
        JTextField archivoUnidadesTextField = this.m_PanelContainer.archivoUnidadesTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("unidades")){
                    archivoUnidadesTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Provincias
        JTextField archivoProvinciasTextField = this.m_PanelContainer.archivoProvinciasTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("provincias")){
                    archivoProvinciasTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Ciudades
        JTextField archivoCiudadesTextField = this.m_PanelContainer.archivoCiudadesTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("ciudades")){
                    archivoCiudadesTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Metatitulo
        JTextField archivoMetatituloTextField = this.m_PanelContainer.archivoMetatituloTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("titulo")){
                    archivoMetatituloTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Metadescripcion
        JTextField archivoMetadescripcionTextField = this.m_PanelContainer.archivoMetadescripcionTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("descripcion")){
                    archivoMetadescripcionTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // Metapalabrasclave
        JTextField archivoMetapalabrasclaveTextField = this.m_PanelContainer.archivoMetapalabrasclaveTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("palabras_clave")){
                    archivoMetapalabrasclaveTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // URL AMIGABLE
        JTextField archivoUrlamigableTextField = this.m_PanelContainer.archivoUrlamigableTextField;
        for (int i = 0; i < numero_archivos; i++){
            Node current_node = archivos_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("uso_archivo").toLowerCase();
                if (uso_archivo.contains("url")){
                    archivoUrlamigableTextField.setText(current_element.getAttribute("nombre").toLowerCase());
                    break;
                }
            }
        }
        
        // BD
        NodeList db_NodeList = m_Document.getElementsByTagName("nodo_db");
        int numero_nodos_db = db_NodeList.getLength();
        
        // URI
        JTextField uriDB_TextField = this.m_PanelContainer.uriDB_TextField;
        for (int i = 0; i < numero_nodos_db; i++){
            Node current_node = db_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("nombre").toLowerCase();
                if (uso_archivo.contains("uri")){
                    uriDB_TextField.setText(current_element.getAttribute("valor").toLowerCase());
                    break;
                }
            }
        }
        
        // USUARIO
        JTextField usuarioDB_TextField = this.m_PanelContainer.usuarioDB_TextField;
        for (int i = 0; i < numero_nodos_db; i++){
            Node current_node = db_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("nombre").toLowerCase();
                if (uso_archivo.contains("usuario")){
                    usuarioDB_TextField.setText(current_element.getAttribute("valor").toLowerCase());
                    break;
                }
            }
        }
        
        // CONTRASEÑA
        JTextField contrasenaDB_TextField = this.m_PanelContainer.contrasenaDB_TextField;
        for (int i = 0; i < numero_nodos_db; i++){
            Node current_node = db_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("nombre").toLowerCase();
                if (uso_archivo.contains("contrasena")){
                    contrasenaDB_TextField.setText(current_element.getAttribute("valor").toLowerCase());
                    break;
                }
            }
        }
        
        // DB NAME
        JTextField DB_name_TextField = this.m_PanelContainer.dB_name_TextField;
        for (int i = 0; i < numero_nodos_db; i++){
            Node current_node = db_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("nombre").toLowerCase();
                if (uso_archivo.contains("db_name")){
                    DB_name_TextField.setText(current_element.getAttribute("valor").toLowerCase());
                    break;
                }
            }
        }
        
        // DB PREFIX
        JTextField DB_prefix_TextField = this.m_PanelContainer.table_prefix_TextField;
        for (int i = 0; i < numero_nodos_db; i++){
            Node current_node = db_NodeList.item(i);
            if (current_node instanceof Element){
                Element current_element = (Element) current_node;
                String uso_archivo = current_element.getAttribute("nombre").toLowerCase();
                if (uso_archivo.contains("db_prefix")){
                    DB_prefix_TextField.setText(current_element.getAttribute("valor").toLowerCase());
                    break;
                }
            }
        }
        
        // PRIMERO VACIAMOS TODOS LOS CAMPOS POR SI SE PUSE ALGO EN EL ULTIMO CAMPO QUE AHORA VAYA A APARECER ARRIBA
        int sustituciones_text_field_size = this.m_PanelContainer.m_Indexed_Sustitution_Seach_String_JtextField_Map.size();
        for (int i = 1; i <= sustituciones_text_field_size; i++){ // aquí comienza en 1
            this.m_PanelContainer.m_Indexed_Sustitution_Seach_String_JtextField_Map.get(i).setText("");
            this.m_PanelContainer.m_Indexed_Sustitution_JtextField_Map.get(i).setText("");
        }
        
        // cargar textfields sustituciones aquí
        NodeList sustituciones_NodeList = m_Document.getElementsByTagName("nodo_sustitucion");
        // Element sustitucionesElement = (Element) sustituciones_NodeList.item(0); // solo hay un elemento

        int sustituciones_size = sustituciones_NodeList.getLength();
        for (int i = 1; i <= sustituciones_size; i++){ // aquí comienza en 1
            JTextField search_string_textfield = this.m_PanelContainer.m_Indexed_Sustitution_Seach_String_JtextField_Map.get(i);
            JTextField sustitution_string_textfield = this.m_PanelContainer.m_Indexed_Sustitution_JtextField_Map.get(i);
            
            if ((search_string_textfield != null) && (sustitution_string_textfield != null)){
                Node current_node = sustituciones_NodeList.item(i-1); // aquí comienza en 0
                String buscar_string = ((Element)current_node).getAttribute("buscar");
                String sustitucion_string = ((Element)current_node).getAttribute("sustituir");
                
                search_string_textfield.setText(buscar_string);
                sustitution_string_textfield.setText(sustitucion_string);
            }
        }
    }
    
    void discardChanges(){
        //JComboBox sustitucionesComboBox = this.m_PanelContainer.sustitucionesComboBox;
        //sustitucionesComboBox.removeAllItems();
        
        // this.load_sustituciones_combobox();
        this.load_text_fields();
    }
    
    private Element getElementDB(String nombre_propiedad_configuracion){
        Element result = null;
        if ((nombre_propiedad_configuracion != null) && (!nombre_propiedad_configuracion.equals(""))){
            NodeList nodo_sustitucion_NodeList = m_Document.getElementsByTagName("nodo_db");
            int numero_nodos_archivo = nodo_sustitucion_NodeList.getLength();
            for (int i = 0; i < numero_nodos_archivo; i++){
                Element elemento_sustitucion = (Element)nodo_sustitucion_NodeList.item(i);
                if (elemento_sustitucion.getAttribute("nombre").equalsIgnoreCase(nombre_propiedad_configuracion)){
                    result = elemento_sustitucion;
                    break;
                }
            }
        }
        return result;
    }
    
    private String readFile(String path, Charset encoding) throws IOException {
        if (encoding == null) encoding = StandardCharsets.UTF_8;
        byte[] encoded = null;
        try {
            encoded = Files.readAllBytes(Paths.get(path));
        }
        catch (NoSuchFileException e){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "No se ha encontrado el archivo "+path);
            return null;
        }
        return new String(encoded, encoding);
    }
    
    private String getCodeBetweenTokens(String originalString, String delimiter1, String delimiter2){
        String result = null;
        
        if ((originalString != null) && (delimiter1 != null)){
            if (!originalString.contains(delimiter1)){
                throw new RuntimeException("delimitador1 no contenido");
            }
            int pos_delimiter1 = originalString.indexOf(delimiter1);
            int pos_delimiter2 = -1;
            // SI delimiter2 no existe pondrá pos_delimiter2 a -1 como ya estaba
            if (delimiter2 != null) pos_delimiter2 = originalString.indexOf(delimiter2);
            /* EL RESULTADO BUSCADO ESTÁ ENTE pos_delimiter1+delimiter1.size y pos_delimiter2
            * o si delimiter2 es null entre pos_delimiter1+delimiter1.size y el final
            * del string
            */
            if (pos_delimiter2 != -1){
                result = originalString.substring(pos_delimiter1+delimiter1.length(), pos_delimiter2);
            }
            else {
                result = originalString.substring(pos_delimiter1+delimiter1.length());
            }
        }
        
        return result;
    }
    
    private String procesar_cuerpo_CMS(String id_construccion_CMS, String[][] estructura_sustituciones) throws IOException{
        
        String idComposicion = id_construccion_CMS != null ? id_construccion_CMS: this.m_PanelContainer.identificadorComposicion_TextField.getText();
        if (!isNumeric(idComposicion)){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Se necesita un número para millares/centenas/decenas y unidades. Puede dejar vacías las primeras posiciones");
        }
        int millares = -1, centenas = -1, decenas = -1, unidades = -1, provincias = -1, ciudades = -1;
        String millares_str = "", centenas_str = "", decenas_str = "", unidades_str = "", provincias_str = "", ciudades_str = "";
        int idCompositionSize = idComposicion.length();
        String separator = null;
        
        for (int i = (idCompositionSize-1); i >= 0; i--){
            String currentFullFileSTR = null;
            String currentFileName = null;
            if (i == (idCompositionSize - (idCompositionSize-3))){
                unidades = getIntegerAtPosition(idComposicion, i);
                currentFileName = getFileName("unidades");
                currentFullFileSTR = readFile(currentFileName,null);
                if (unidades < 10){
                    unidades_str = getCodeBetweenTokens(currentFullFileSTR, "##UNIDADES 0"+unidades+"##", "##UNIDADES 0"+(unidades+1)+"##");
                }
                else {
                    unidades_str = getCodeBetweenTokens(currentFullFileSTR, "##UNIDADES "+unidades+"##", "##UNIDADES "+(unidades+1)+"##");
                }                
            }
            else if (i == (idCompositionSize - (idCompositionSize-2))){
                decenas = getIntegerAtPosition(idComposicion, i);
                currentFileName = getFileName("decenas");
                currentFullFileSTR = readFile(currentFileName,null);
                if (decenas < 10){
                    decenas_str = getCodeBetweenTokens(currentFullFileSTR, "##DECENAS 0"+decenas+"##", "##DECENAS 0"+(decenas+1)+"##");
                }
                else {
                    decenas_str = getCodeBetweenTokens(currentFullFileSTR, "##DECENAS "+decenas+"##", "##DECENAS "+(decenas+1)+"##");
                }
            }
            else if (i == (idCompositionSize - (idCompositionSize-1))){
                centenas = getIntegerAtPosition(idComposicion, i);
                currentFileName = getFileName("centenas");
                currentFullFileSTR = readFile(currentFileName,null);
                if (centenas < 10){
                    centenas_str = getCodeBetweenTokens(currentFullFileSTR, "##CENTENAS 0"+centenas+"##", "##CENTENAS 0"+(centenas+1)+"##");
                }
                else {
                    centenas_str = getCodeBetweenTokens(currentFullFileSTR, "##CENTENAS "+centenas+"##", "##CENTENAS "+(centenas+1)+"##");
                }
            }
            else if (i == (idCompositionSize - (idCompositionSize) )){
                millares = getIntegerAtPosition(idComposicion, i);
                currentFileName = getFileName("millares");
                currentFullFileSTR = readFile(currentFileName,null);
                if (millares < 10){
                    millares_str = getCodeBetweenTokens(currentFullFileSTR, "##MILLAR 0"+millares+"##", "##MILLAR 0"+(millares+1)+"##");
                }
                else {
                    millares_str = getCodeBetweenTokens(currentFullFileSTR, "##MILLAR "+millares+"##", "##MILLAR "+(millares+1)+"##");
                }
                
                // AHORA TOCA CIUDADES
                currentFileName = getFileName("ciudades");
                currentFullFileSTR = readFile(currentFileName,null);
                separator = "##CIUDADES XX##";
                ciudades = getIndexOfItemInFile(millares, separator, currentFullFileSTR);
                if (ciudades < 10){
                    ciudades_str = getCodeBetweenTokens(currentFullFileSTR, "##CIUDADES 0"+ciudades+"##", "##CIUDADES 0"+(ciudades+1)+"##");
                }
                else {
                    ciudades_str = getCodeBetweenTokens(currentFullFileSTR, "##CIUDADES "+ciudades+"##", "##CIUDADES "+(ciudades+1)+"##");
                }
                // AHORA TOCA PROVINCIAS
                currentFileName = getFileName("provincias");
                currentFullFileSTR = readFile(currentFileName,null);
                separator = "##PROVINCIAS XX##";
                provincias = getIndexOfItemInFile(millares, separator, currentFullFileSTR);
                if (provincias < 10){
                    provincias_str = getCodeBetweenTokens(currentFullFileSTR, "##PROVINCIAS 0"+provincias+"##", "##PROVINCIAS 0"+(provincias+1)+"##");
                }
                else {
                    provincias_str = getCodeBetweenTokens(currentFullFileSTR, "##PROVINCIAS "+provincias+"##", "##PROVINCIAS "+(provincias+1)+"##");
                }               
            }
        }
        
        return this.ejecuta_sustituciones(millares_str + centenas_str + decenas_str + unidades_str + ciudades_str + provincias_str, estructura_sustituciones);
    }
    
    private boolean isNumeric(String str){
        if (str.equals("")) return false;
        for (char c : str.toCharArray())
        {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
    
    private int getIntegerAtPosition(String str, int position){
        if ((str == null) || (position < 0)){
            throw new RuntimeException("la cadena pasada no puede ser nula ni position igual o menor a cero");
        }
        
        try {
            if (position == str.length()){
                return Integer.parseInt(str.substring(position));
            }
            else {
                return Integer.parseInt(str.substring(position, position+1));

                //return Integer.parseInt(str.substring(position-1, position));
            }
        }
        catch (IndexOutOfBoundsException e){
            
            Logger.getLogger(PanelGrafico.class.getName()).log(Level.SEVERE, null, e);
            return -1;
        }
    }
    
    private String getFileName (String use){
        if (use == null){
            throw new RuntimeException("la cadena pasada no puede ser nula");
        }
        Element element = getElementArchivos(use);
        if (element == null){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "no se encontró el elemento que representa al archivo de "+use);
            return null;
        }
        else {
            return element.getAttribute("nombre");
        }
    }
    
    private void save_To_File(String filename, String string_to_save) throws FileNotFoundException{
        if ((filename == null) || (filename.equals("")) || (string_to_save == null) || (string_to_save.equals(""))){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "la cadena pasada no puede ser nula o estar vacía, ni el nombre de archivo");
            return ;
            // throw new RuntimeException("la cadena pasada no puede ser nula o estar vacía, ni el nombre de archivo");
        }
        PrintWriter out = new PrintWriter(filename);
        out.println(string_to_save);
        out.close();
    }
    
    private String ejecuta_sustituciones(String texto_sin_sustituciones, String[][] estructura_sustituciones){
        String result = null, partial_result = null, current_search_string = null, current_sustitution_string = null;
        if ((texto_sin_sustituciones != null) && (texto_sin_sustituciones.length() > 0)){
            String[][] lista_sustituciones = null;
            if (estructura_sustituciones == null){
                lista_sustituciones = crear_lista_sustituciones(null, null);
            }
            else {
                lista_sustituciones = estructura_sustituciones;
            }
            
            if (lista_sustituciones != null){
                for (int i = 0; i < lista_sustituciones.length; i++){
                    current_search_string = lista_sustituciones[i][0];
                    current_sustitution_string = lista_sustituciones[i][1];
                    if (partial_result != null){
                        String patrón = "(?i)"+Pattern.quote(current_search_string);
                        partial_result = partial_result.replaceAll(patrón, current_sustitution_string);
                    }
                    else {
                        String patrón = "(?i)"+Pattern.quote(current_search_string);
                        partial_result = texto_sin_sustituciones.replaceAll(patrón, current_sustitution_string);
                    }
                }
            }
            
            result = partial_result;
        }
        else {
            result = "";
        }
        return result;
    }
    
    private int getIndexOfItemInFile(int unidades, String separator, String fileString){
        
        String current_separator = null;
        boolean endOfFile = false;
        int max_found_index = 0;
        
        while (!endOfFile){
            if (max_found_index < 10){
                current_separator = separator.replaceAll("XX", "0"+max_found_index);
                
            }
            else {
                current_separator = separator.replaceAll("XX", String.valueOf(max_found_index));
                
            }
            int firstIndexOfSeparator = fileString.indexOf(current_separator);
            if (firstIndexOfSeparator == -1) {
                endOfFile = true;
            }
            else {
                max_found_index ++;
            }
        }
        
        if (max_found_index > unidades){
            return unidades;
        }
        else {
            // devolvemos el resto de dividir unidades entre max_found_index: operación MOD
            if (max_found_index == 0){
                JOptionPane.showMessageDialog(this.m_ConfigFrame, "No se han detectado cadenas de búsqueda del tipo "+separator+" en el archivo");
                return -1;
            }
            return unidades % max_found_index;
        }
    }
    
    private boolean existFolder(String folder){
        boolean result = false;
        if ((folder != null) && (folder.length() > 0)){
            Path folder_path = Paths.get(folder);
            if (Files.exists(folder_path)) {
                result = true;
            }
        }
        return result;
    }
    
    private void createFolder(String folder){
        if ((folder != null) && (folder.length() > 0)){          
            new File(folder).mkdirs();
        }
        else {
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "no se puede crear la carpeta de resultados");
        }
    }
    
    private ArrayList<Node> extractTextChildrenNodes(Element parentNode) {
        NodeList childNodes = parentNode.getChildNodes();
        ArrayList<Node> result = new ArrayList<Node>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                result.add(node);
            }
            else {
                if (node.hasChildNodes()){
                    NodeList son_childNodes = parentNode.getChildNodes();
                    for (int innerIndex = 0; innerIndex < son_childNodes.getLength(); innerIndex ++){
                        Node innerNode = son_childNodes.item(innerIndex);
                        if (innerNode instanceof Element){
                            result.addAll(extractTextChildrenNodes((Element)innerNode));
                        }
                    }
                }
            }
        }
        return result;
    }
    private void removePassedNodes(ArrayList<Node> list_of_nodes){
        if ((list_of_nodes != null) && (list_of_nodes.size() > 0 )){
            int list_size = list_of_nodes.size();
            for (int i = 0; i < list_size; i++){
                Node node = list_of_nodes.get(i);
                Node parent_node = node.getParentNode();
                /*
                if (parent_node == null){
                    String toString = node.toString();
                    JOptionPane.showMessageDialog(this.m_ConfigFrame, "este es el toString del nodo cuyo padre es null: "+toString);
                }
                */
                if (parent_node != null){
                    parent_node.removeChild(node);
                }
            }
        }
    }
    
    
    private void connectMysqlDB() throws SQLException{
        if(m_Connection == null){
            MysqlDataSource dataSource = new MysqlDataSource();
            String usuario = this.getElementDB("usuario").getAttribute("valor");
            dataSource.setUser(usuario);
            String contrasena = this.getElementDB("contrasena").getAttribute("valor");
            dataSource.setPassword(contrasena);
            String uri = this.getElementDB("uri").getAttribute("valor");
            dataSource.setServerName(uri);
            m_Connection = (Connection) dataSource.getConnection();
            m_Connection.setCatalog(this.getElementDB("db_name").getAttribute("valor"));
            m_Statement = (Statement) m_Connection.createStatement();
        }
    }
   
    private void executeQuery(String query) throws SQLException{
        if ((query != null) && (!query.equals(""))){
            m_Last_ResultSet = m_Statement.executeQuery(query);
        }
    }
    
    private int executeInsert(String insert_SQL) throws SQLException{
        if ((insert_SQL != null) && (!insert_SQL.equals(""))){
            return m_Statement.executeUpdate(insert_SQL);
        }
        return 0;
    }
    
    private void closeMysqlDB_connection() throws SQLException{
        try {
            m_Last_ResultSet.close();
            m_Statement.close();
            m_Connection.close();
        }
        
        catch (SQLException e){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "No se pudo cerrar la conexión a la base de datos");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
    }
    
    private Map<String,String> procesarCMS_completo(String id_construccion_CMS, String[][] estructura_sustituciones) throws IOException{
        String codigo_cms = this.procesar_cuerpo_CMS(id_construccion_CMS, estructura_sustituciones);
        String meta_titulo = readFile(getFileName("titulo"), null);
        String meta_palabras_clave = readFile(getFileName("palabras_clave"), null);
        String meta_descripcion = readFile(getFileName("descripcion"), null);
        String url_amigable = readFile(getFileName("url"), null);
        
        meta_titulo = ejecuta_sustituciones(meta_titulo, estructura_sustituciones);
        meta_palabras_clave = ejecuta_sustituciones(meta_palabras_clave, estructura_sustituciones);
        meta_descripcion = ejecuta_sustituciones(meta_descripcion, estructura_sustituciones);
        url_amigable = ejecuta_sustituciones(url_amigable, estructura_sustituciones);
 
        Map<String, String> result = new HashMap<String,String>();
        result.put("cms", codigo_cms);
        result.put("titulo", meta_titulo);
        result.put("palabras_clave", meta_palabras_clave);
        result.put("descripcion", meta_descripcion);
        url_amigable = url_amigable.trim();
        String new_url = Normalizer.normalize(url_amigable.toLowerCase().replaceAll(" ", "-").replaceAll("'",""), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        result.put("url", new_url);
        
        return result;
    }
    
    private void leer_archivo_automatizacion_por_lotes() throws FileNotFoundException, IOException{
        String filename = this.getElementArchivos("automatizacion").getAttribute("nombre");
        
        // CONTAMOS EL NUMERO DE LINEAS
        LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(filename)));
        lnr.skip(Long.MAX_VALUE);
        int number_of_lines = lnr.getLineNumber() + 1; //Add 1 because line index starts at 0
        // Finally, the LineNumberReader object should be closed to prevent resource leak
        lnr.close();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), "UTF-8"))) {
            m_Automatization_File = new String[number_of_lines][];
            int counter = 0;
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                m_Automatization_File[counter] = line.split(",", -1);
                counter++;
            }
            // line is not visible here.
            br.close();
            
        }
        /*
        catch (Exception e){
            String cadena = e.getLocalizedMessage()+"\n"+e.getMessage()+"\n"+e.getCause()+"\n";
            StackTraceElement[] arrayError = e.getStackTrace();
            for (int i = 0; i < arrayError.length; i++){
                cadena += arrayError[i].toString()+"\n";
            }
            JOptionPane.showMessageDialog(this.m_ConfigFrame, cadena);
            System.exit(-1);
        }
        
        /* CODIGO DE DEPURACION */
        /*
        String cadena = "";
        for (int i = 0; i < m_Automatization_File[0].length; i++){
            //cadena += cadena;
            // cadena += "palabra a sustituir: "+m_Automatization_File[0][i];
            cadena += " se sustitituye por: "+m_Automatization_File[80][i]+"\n";
        }
        JOptionPane.showMessageDialog(this.m_ConfigFrame, cadena);
        System.exit(-1);
        /* FIN CODIGO DEPURACION */
    }
    
    private void setAutomatizationFileHeader(){
        if ((m_Automatization_File == null) || (m_Automatization_File.length == 0)){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "El archivo de automatización no se ha cargado o no tiene el formato correcto");
            return;
        }
        String[] header = m_Automatization_File[0];
        if (header.length == 0){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "La cabecera del archivo de automatización no tiene el formato correcto");
            return;
        }
        for (int i = 0; i < header.length; i++){
            if (header[i].matches("^#.*#$")){
                this.m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion.put(header[i], i);
            }
        }
    }
    
    private void procesarLote() throws IOException{
        if ((m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion == null) || (m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion.size() == 0)){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "El archivo de automatización no ha sido cargado. Indique donde está el archivo y pulse 'Cargar archivo por lotes'");
            return;
        }
        // emitir aviso si alguna fila no contiene id cms o id de creación de cms
        String cms_lotes_desde_TextField_string = this.m_PanelContainer.cms_lotes_desde_TextField.getText();
        String cms_lotes_hasta_TextField_string = this.m_PanelContainer.cms_lotes_hasta_TextField.getText();
        String id_tienda = this.m_PanelContainer.id_tienda_lotes_TextField.getText();
        
        String idioma_proceso_lotes = this.m_PanelContainer.idioma_proceso_lotes_TextField.getText();
        
        String cms_lotes_limite_procesamiento_string = this.m_PanelContainer.limite_seguridad_CMS_lotes_TextField.getText();
        
        if ((!this.isNumeric(cms_lotes_desde_TextField_string)) || (!this.isNumeric(cms_lotes_hasta_TextField_string))){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Debe introducir números en los campos desde y hasta, empezando por 1 como mínimo y hasta el número máximo de cms procesados");
            return;
        }
        
        if (!this.isNumeric(idioma_proceso_lotes)){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Debe introducir un número identificador de idioma válido");
            return;
        }
        
        if (!this.isNumeric(cms_lotes_limite_procesamiento_string)){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Debe introducir un número que especifique un límite de subida de CMS válido");
            return;
        }
                
        int desde = Integer.parseInt(cms_lotes_desde_TextField_string);
        int hasta = Integer.parseInt(cms_lotes_hasta_TextField_string);
        int limite = Integer.parseInt(cms_lotes_limite_procesamiento_string);

        // int idioma = Integer.parseInt(idioma_proceso_lotes);
        
        
        if (desde < 0 || hasta < 0){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Los números desde o hasta no pueden ser negativos");
            return;
        }
        
        if (desde > hasta){
            int temp = desde;
            desde = hasta;
            hasta = temp;
        }
        
        if (limite <= (hasta-desde)){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "Ha superado el límite de procesamiento que ha configurado. No se procesarán los CMS");
            return;
        }
        
        
        int idCMS_index = this.m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion.get("#IDCMS#");
        int idCreacionCMS_index = this.m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion.get("#ICODE#");
        int idCategoryCMS_index = this.m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion.get("#IDCMSCATEGORY#");
        
        Set<Entry<String,Integer>> conjunto_entradas_mapa = this.m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion.entrySet();
        
        int tamano_array_sustitucion = conjunto_entradas_mapa.size() - 2; // idcms e idcreacioncms no cuentan
        boolean mostrar_mensaje_emergente = false;
        ArrayList<Object> lista_resultados = new ArrayList<Object>();
        for (int i = desde; i <= hasta; i++){
            String[] fila_actual_proceso = this.m_Automatization_File[i];
            
            if ((!this.isNumeric(fila_actual_proceso[idCMS_index])) || (!this.isNumeric(fila_actual_proceso[idCreacionCMS_index]))){
                JOptionPane.showMessageDialog(this.m_ConfigFrame, "La fila "+i+" del archivo de automatización no tiene un id de cms o id de creación de cms correcto");
                continue;
            }
            
            String idCMS = fila_actual_proceso[idCMS_index];
            String idCreacionCMS = fila_actual_proceso[idCreacionCMS_index];
            String idCategoryCMS = fila_actual_proceso[idCategoryCMS_index];
            
            String[][] lista_sustitucion_actual = crear_lista_sustituciones(m_Mapa_Cabecera_Identificador_Indice_Archivo_Automatizacion, i);
            /* Parametros que se pueden pasar a processAction para la acción "procesar y subir cms":
            * args[0] idCMS
            * args[1] idIdioma
            * args[2] idConstruccionCMS
            * args[3] estructura de sustituciones
            * args[4] idCategory
            *
            * Si algún parámetro no se pasa se entiende que el procesamiento es
            * de los cms individuales y se cogerá el textField correspondiente
            * a la creación de dicho CMS
            *
            */
            Object array_resultado = this.processAction("procesar_y_subir_cms", idCMS, idioma_proceso_lotes, idCreacionCMS, lista_sustitucion_actual, mostrar_mensaje_emergente, idCategoryCMS, id_tienda);
            lista_resultados.add(array_resultado);
        }
        int insertados = contarLotes(lista_resultados, "INSERTADO");
        int actualizados = contarLotes(lista_resultados, "ACTUALIZADO");
        String detalles = obtenerDetallesLotes(lista_resultados);
        this.save_To_File("lotes.log", detalles);
        JOptionPane.showMessageDialog(this.m_ConfigFrame, "Finalizado proceso por lotes\nINSERTADOS: "
                +insertados+" CMS\nACTUALIZADOS: "+ actualizados+ " CMS\nDetalles del proceso en archivo lotes.log");
    }
    
    public String[][] crear_lista_sustituciones(Map<String,Integer> mapa_lotes_automatico, Integer linea_proceso_lotes_actual){
        String[][] result = null;
        NodeList nodeList_sustituciones = null;
        String[] linea_datos_proceso_lotes = null;
                
        Entry[] entrySetArray = null;
        int listSize;
        
        if (mapa_lotes_automatico == null){
            nodeList_sustituciones = m_Document.getElementsByTagName("nodo_sustitucion");
            listSize = nodeList_sustituciones.getLength();
        }
        else {
            listSize = mapa_lotes_automatico.size() - 2;
            if (linea_proceso_lotes_actual == null){
                throw new RuntimeException("crear_lista_sustituciones: linea_proceso_lotes_actual no puede ser nulo");
            }
            linea_datos_proceso_lotes = this.m_Automatization_File[linea_proceso_lotes_actual];
            entrySetArray = mapa_lotes_automatico.entrySet().toArray(new Entry[listSize]);
        }
        
        // int listSize = mapa_lotes_automatico != null? : 

        int index_linea_proces_lotes_actual = 0;
        // Set<Entry<String,Integer>> entrySet_name_index = null;

        if ((nodeList_sustituciones != null) || (mapa_lotes_automatico != null)) {
            
            int innerIndex = 0;
            result = new String[listSize][2]; // 2: cadena a buscar, cadena a sustituir
            for (int i = 0; i < result.length+2; i ++){ // +2 campos solo para proceso automático por csv
                String current_search_string = null;
                String current_sustitution_string = null;
                if (mapa_lotes_automatico == null){
                    Element current_sustitution = (Element) nodeList_sustituciones.item(innerIndex);
                    if (current_sustitution != null) { // si es null no tenemos más campos
                        current_search_string = current_sustitution.getAttribute("buscar");
                        current_sustitution_string = current_sustitution.getAttribute("sustituir");
                    }
                }
                else {
                    if (
                            ((String)entrySetArray[index_linea_proces_lotes_actual].getKey()).equalsIgnoreCase("#IDCMS#")
                                ||
                            ((String)entrySetArray[index_linea_proces_lotes_actual].getKey()).equalsIgnoreCase("#ICODE#")){
                        
                        index_linea_proces_lotes_actual++;
                        continue;
                    }
                    
                    current_search_string = (String)entrySetArray[index_linea_proces_lotes_actual].getKey();
                    current_sustitution_string = linea_datos_proceso_lotes[(Integer)entrySetArray[index_linea_proces_lotes_actual].getValue()];
                    
                    index_linea_proces_lotes_actual++;
                }
                if (innerIndex < result.length){ // si el indice actual está dentro de 
                    // int integerrr = 1;
                
                    result[innerIndex][0] = current_search_string;
                    result[innerIndex][1] = current_sustitution_string;

                    innerIndex++;
                }
            }
        }
        return result;
    }
    
    private void cerrarSistema(){
        
        try {
            this.closeMysqlDB_connection();
        }
        catch (SQLException e){
            JOptionPane.showMessageDialog(this.m_ConfigFrame, "No se pudo cerrar la conexión a la base de datos");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int contarLotes(ArrayList<Object> lista_resultado_lotes, String cadena_busqueda){
        int resultado = 0;
        if ((lista_resultado_lotes != null) && (cadena_busqueda!=null)){
            for (int i = 0; i < lista_resultado_lotes.size(); i++){
                Object[] arr_result = (Object[]) lista_resultado_lotes.get(i);
                if (((String)arr_result[0]).equalsIgnoreCase(cadena_busqueda)){
                    resultado++;
                }
            }
        }
        return resultado;
    }
    
    private String obtenerDetallesLotes(ArrayList<Object> lista_resultados){
        String result = "";
        if (lista_resultados != null){
            for (int i = 0; i < lista_resultados.size(); i++){
                Object[] arr_result = (Object[]) lista_resultados.get(i);
                String cadena_resultado_actual = ((String)arr_result[1])+"\r\n";
                result += cadena_resultado_actual;
            }
        }
        return result;
    }

}

