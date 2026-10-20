import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
public class Main {
    
    private static String usuario;
    private static String contraseña;
    private static String url;
    
    public void main(String[] args) {
        Properties p = new Properties();
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("Error al leer config.properties: " + e.getMessage());
        }
        try (Scanner sc = new Scanner(System.in)) {
            iniciarSesion(sc, p);
            System.exit(0);
        }
    }
    
    private void iniciarSesion(Scanner scanner, Properties p) {
        while (true) { 
            System.out.println("\n====== INCIAR SESIÓN ======");
            usuario = leerString(scanner, "Usuario: ");
            contraseña = leerString(scanner, "Contraseña: ");
            url = p.getProperty("db.url");
            if (p.getProperty(usuario) == null) {
                System.out.println("Error: Usuario introducido no existe");
            } else if (!p.getProperty(usuario).equals(contraseña)) {
                System.out.println("Error: Contraseña incorrecta");
            } else {
                mostarMenu(scanner, usuario, contraseña, url);
                break;
            }
        }
        
    }
    
    public static void mostarMenu(Scanner scanner, String usuario, String contraseña, String url) {
        int opcion;
        do {
            System.out.println("\n====== MENU ======");
            System.out.println("1. CRM");
            System.out.println("2. Ventas");
            System.out.println("3. Compras");
            System.out.println("4. Fabricación");
            System.out.println("0. Salir");
            opcion = leerInt(scanner, "> ");
            ejecutar(usuario, contraseña, url, opcion);
        } while (opcion != 0);
    }
    
    private static void ejecutar(String usuario, String contraseña, String url, int opcion) {
        switch (opcion) {
            case 1 -> {
                if (usuario.equals("deca_crm") || usuario.equals("deca_admin")) {
                    Ventas ventas = new Ventas(usuario, contraseña, url);
                    ventas.cargarVentas(usuario, contraseña, url);
                }
            }
            case 2 -> {
                if (usuario.equals("deca_ventas") || usuario.equals("deca_admin")) {
                    Ventas ventas = new Ventas(usuario, contraseña, url);
                    ventas.cargarVentas(usuario, contraseña, url);
                }
            }
            case 3 -> {
                if (usuario.equals("deca_compras") || usuario.equals("deca_admin")) {
                    Compra compra = new Compra(usuario, contraseña, url);
                    compra.cargarCompra(usuario, contraseña, url);
                }
            }
                case 4 -> {
    if (usuario.equals("deca_fabricacion")
            || usuario.equals("deca_admin")) {

        Fabricacion fabricacion =
                new Fabricacion(usuario, contraseña, url);

        fabricacion.cargarFabricacion(usuario, contraseña, url);

    } else {
        System.out.println(
                "Error: No tienes permisos para Fabricación.");
    }
}
            case 0 -> System.exit(0);
            default -> System.out.println("Error: Opción introducida incorrecta");
        }
    }
    
    /* private void probarConexion(Properties p, String usuario, String contraseña) {
        try (Connection conn = DriverManager.getConnection(
            p.getProperty("db.url"), usuario, contraseña )) {
            System.out.println("Conexión exitosa");
        } catch (SQLException e) {System.out.println("Error: " + e.getMessage());}
    } */
    
    public static void imprimirLinea(String[] valores, int[] anchos) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            String valor = valores[i];
            linea.append("| ");
            linea.append(String.format("%-" + anchos[i] + "s", valor));
            linea.append(" ");
        }
        linea.append("|");
        System.out.println(linea);
    }
    
    public static void imprimirSeparador(int[] anchos) {
        StringBuilder linea = new StringBuilder();
        for (int ancho : anchos) {
            linea.append("|-");
            linea.append("-".repeat(ancho + 1));
        }
        linea.append("|");
        System.out.println(linea);
    }
    
    public static String[] recogerCabeceras(ResultSetMetaData md, int columnas) {
        String[] cabeceras = new String[columnas];
        try {
            for (int i = 0; i < columnas; i++) {
                cabeceras[i] = md.getColumnLabel(i + 1);
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
        return cabeceras;
    }
    
    public static ArrayList<String[]> recogerDatos(ResultSet rs, int columnas) {
        ArrayList<String[]> filas = new ArrayList<>();
        try {
            while (rs.next()) {
                String[] fila = new String[columnas];
            for (int i = 0; i < columnas; i++) {
                fila[i] = rs.getString(i + 1);
            }
            filas.add(fila);
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage());}
        return filas;
    }
    
    public static int[] calcularAnchos(int columnas, String[] cabeceras, ArrayList<String[]> filas) {
        int[] anchos = new int[columnas];
        for (int i = 0; i < columnas; i++) {
            anchos[i] = cabeceras[i].length();
            for (String[] fila : filas) {
                if (fila[i] != null && fila[i].length() > anchos[i]) {
                    anchos[i] = fila[i].length();
                }
            }
        }
        return anchos;
    }
    
    public static String leerString(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        String entrada = scanner.nextLine().trim();
        return entrada;
    }
    
    public static int leerInt(Scanner scanner, String mensaje) {
        while (true) { 
            System.out.print(mensaje);
            String entrada = scanner.nextLine().trim();
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {System.out.println("Error:" + e.getMessage());}
        }
    }
    
    public static Boolean verificarId(int id, String tabla) {
        try (Connection conn = DriverManager.getConnection(url, usuario, contraseña)) {
            PreparedStatement st = conn.prepareStatement("SELECT * FROM " + tabla + " WHERE id = ?");
            st.setInt(1, id);
            
            ResultSet rs = st.executeQuery();
            return rs.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return false;
    }
}

// aplicacion = funciona;
// estetica = bonito;
// funciones = muchas;
