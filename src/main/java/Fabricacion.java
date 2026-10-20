```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Fabricacion {

    private final String usuario;
    private final String contraseña;
    private final String url;

    public Fabricacion(String usuario, String contraseña, String url) {
        this.usuario = usuario;
        this.contraseña = contraseña;
        this.url = url;
    }

    public void cargarFabricacion(String usuario, String contraseña, String url) {

        if (!usuario.equals("deca_fabricacion")
                && !usuario.equals("deca_admin")) {
            System.out.println("Error: No tienes permisos para Fabricación.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(
                url, usuario, contraseña)) {

            System.out.println("\n====== DEPARTAMENTO DE FABRICACIÓN ======");
            System.out.println("1. Consultar fabricación");
            System.out.println("0. Volver al menú");

            try (Scanner sc = new Scanner(System.in)) {
                System.out.print("> ");
                String opcion = sc.nextLine().trim();

                switch (opcion) {
                    case "1" -> consultarFabricacion(conn);
                    case "0" -> System.out.println("Volviendo al menú...");
                    default -> System.out.println("Error: Opción incorrecta.");
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error de conexión o consulta SQL: " + e.getMessage());
        }
    }

    private void consultarFabricacion(Connection conn) {

        String sql = "SELECT * FROM Fabricacion";

        try (PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            ResultSetMetaData md = rs.getMetaData();
            int columnas = md.getColumnCount();

            String[] cabeceras = new String[columnas];

            for (int i = 0; i < columnas; i++) {
                cabeceras[i] = md.getColumnLabel(i + 1);
            }

            ArrayList<String[]> filas = new ArrayList<>();

            while (rs.next()) {
                String[] fila = new String[columnas];

                for (int i = 0; i < columnas; i++) {
                    fila[i] = rs.getString(i + 1);

                    if (fila[i] == null) {
                        fila[i] = "NULL";
                    }
                }

                filas.add(fila);
            }

            int[] anchos = new int[columnas];

            for (int i = 0; i < columnas; i++) {
                anchos[i] = cabeceras[i].length();

                for (String[] fila : filas) {
                    anchos[i] = Math.max(anchos[i], fila[i].length());
                }
            }

            imprimirSeparador(anchos);
            imprimirLinea(cabeceras, anchos);
            imprimirSeparador(anchos);

            for (String[] fila : filas) {
                imprimirLinea(fila, anchos);
            }

            imprimirSeparador(anchos);

            System.out.println("Registros encontrados: " + filas.size());

        } catch (SQLException e) {
            System.out.println(
                    "Error al consultar Fabricación: " + e.getMessage());
        }
    }

    private void imprimirLinea(String[] valores, int[] anchos) {
        StringBuilder linea = new StringBuilder();

        for (int i = 0; i < valores.length; i++) {
            linea.append("| ");
            linea.append(String.format("%-" + anchos[i] + "s", valores[i]));
            linea.append(" ");
        }

        linea.append("|");
        System.out.println(linea);
    }

    private void imprimirSeparador(int[] anchos) {
        StringBuilder linea = new StringBuilder();

        for (int ancho : anchos) {
            linea.append("|-");
            linea.append("-".repeat(ancho + 1));
        }

        linea.append("|");
        System.out.println(linea);
    }
}
```
