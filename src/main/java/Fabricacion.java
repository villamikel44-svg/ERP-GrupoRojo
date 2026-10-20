
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        Scanner sc = new Scanner(System.in);
        int opcion;

        do {
            mostrarMenu();
            opcion = Main.leerInt(sc, "> ");
            ejecutarFabricacion(opcion, sc);
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("\n====== FABRICACIÓN ======");
        System.out.println("1. Crear orden de fabricación");
        System.out.println("2. Listar órdenes de fabricación");
        System.out.println("3. Iniciar orden de fabricación");
        System.out.println("4. Finalizar orden y actualizar stock");
        System.out.println("5. Cancelar orden de fabricación");
        System.out.println("0. Volver");
    }

    private void ejecutarFabricacion(int opcion, Scanner sc) {
        switch (opcion) {
            case 1 -> crearOrden(sc);
            case 2 -> listarOrdenes();
            case 3 -> iniciarOrden(sc);
            case 4 -> finalizarOrden(sc);
            case 5 -> cancelarOrden(sc);
            case 0 -> System.out.println("Volviendo al menú principal...");
            default -> System.out.println("Error: opción incorrecta");
        }
    }

    // 1. Crear orden de fabricación
    private void crearOrden(Scanner sc) {
        System.out.println("\n====== NUEVA ORDEN DE FABRICACIÓN ======");

        int idProducto = Main.leerInt(sc,
                "ID del producto que se fabricará: ");
        int cantidad = Main.leerInt(sc,
                "Cantidad que se fabricará: ");

        if (cantidad <= 0) {
            System.out.println("Error: la cantidad debe ser mayor que cero.");
            return;
        }

        String sqlProducto =
                "SELECT id FROM productos WHERE id = ?";

        String sqlMateriales =
                "SELECT COUNT(*) FROM materiales_fabricacion "
                + "WHERE producto_final_id = ?";

        String sqlInsertar =
                "INSERT INTO ordenes_fabricacion "
                + "(producto_id, cantidad, estado) "
                + "VALUES (?, ?, 'pendiente')";

        try (Connection conn =
                DriverManager.getConnection(url, usuario, contraseña)) {

            try (PreparedStatement ps =
                    conn.prepareStatement(sqlProducto)) {

                ps.setInt(1, idProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Error: el producto no existe.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sqlMateriales)) {

                ps.setInt(1, idProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println(
                                "Error: el producto no tiene materiales definidos.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps =
                    conn.prepareStatement(sqlInsertar)) {

                ps.setInt(1, idProducto);
                ps.setInt(2, cantidad);
                ps.executeUpdate();
            }

            System.out.println(
                    "Orden de fabricación creada correctamente.");

        } catch (SQLException e) {
            System.out.println(
                    "Error al crear la orden: " + e.getMessage());
        }
    }

    // 2. Listar órdenes de fabricación
    private void listarOrdenes() {
        System.out.println("\n====== ÓRDENES DE FABRICACIÓN ======");

        String sql = """
                SELECT o.id, o.producto_id, p.nombre,
                       o.cantidad, o.estado,
                       o.fecha_creacion, o.fecha_finalizacion
                FROM ordenes_fabricacion o
                INNER JOIN productos p ON p.id = o.producto_id
                ORDER BY o.id
                """;

        try (Connection conn =
                DriverManager.getConnection(url, usuario, contraseña);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean hayDatos = false;

            while (rs.next()) {
                hayDatos = true;

                System.out.println("-----------------------------------");
                System.out.println("OF: " + rs.getInt("id"));
                System.out.println("Producto: " + rs.getString("nombre"));
                System.out.println("ID producto: "
                        + rs.getInt("producto_id"));
                System.out.println("Cantidad: " + rs.getInt("cantidad"));
                System.out.println("Estado: " + rs.getString("estado"));
                System.out.println("Creación: "
                        + rs.getTimestamp("fecha_creacion"));
                System.out.println("Finalización: "
                        + rs.getTimestamp("fecha_finalizacion"));
            }

            if (!hayDatos) {
                System.out.println(
                        "No hay órdenes de fabricación.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al listar órdenes: " + e.getMessage());
        }
    }

    // 3. Iniciar una orden pendiente
    private void iniciarOrden(Scanner sc) {
        System.out.println("\n====== INICIAR FABRICACIÓN ======");

        int id = Main.leerInt(sc, "ID de la orden: ");

        String sql = """
                UPDATE ordenes_fabricacion
                SET estado = 'en proceso'
                WHERE id = ? AND estado = 'pendiente'
                """;

        try (Connection conn =
                DriverManager.getConnection(url, usuario, contraseña);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            if (ps.executeUpdate() == 1) {
                System.out.println(
                        "Orden iniciada correctamente.");
            } else {
                System.out.println(
                        "No existe la orden o no está pendiente.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al iniciar la orden: " + e.getMessage());
        }
    }

    // 4. Finalizar orden y actualizar existencias
    private void finalizarOrden(Scanner sc) {
        System.out.println("\n====== FINALIZAR FABRICACIÓN ======");

        int id = Main.leerInt(sc, "ID de la orden: ");

        String confirmacion = Main.leerString(
                sc,
                "¿Confirmas la fabricación y el cambio de stock? (y/n): ");

        if (!confirmacion.equalsIgnoreCase("y")) {
            System.out.println("Operación cancelada.");
            return;
        }

        try (Connection conn =
                DriverManager.getConnection(url, usuario, contraseña)) {

            conn.setAutoCommit(false);

            try {
                int idProducto;
                int cantidad;

                String sqlOrden = """
                        SELECT producto_id, cantidad
                        FROM ordenes_fabricacion
                        WHERE id = ? AND estado = 'en proceso'
                        FOR UPDATE
                        """;

                try (PreparedStatement ps =
                        conn.prepareStatement(sqlOrden)) {

                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "La orden no existe o no está en proceso.");
                        }

                        idProducto = rs.getInt("producto_id");
                        cantidad = rs.getInt("cantidad");
                    }
                }

                String sqlMateriales = """
                        SELECT material_id, cantidad_por_unidad
                        FROM materiales_fabricacion
                        WHERE producto_final_id = ?
                        """;

                boolean hayMateriales = false;

                try (PreparedStatement ps =
                        conn.prepareStatement(sqlMateriales)) {

                    ps.setInt(1, idProducto);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            hayMateriales = true;

                            int idMaterial = rs.getInt("material_id");
                            int porUnidad =
                                    rs.getInt("cantidad_por_unidad");

                            long necesario = (long) porUnidad * cantidad;

                            if (necesario <= 0
                                    || necesario > Integer.MAX_VALUE) {
                                throw new SQLException(
                                        "Cantidad de material no válida.");
                            }

                            String sqlDescontar = """
                                    UPDATE existencias
                                    SET cantidad = cantidad - ?
                                    WHERE producto_id = ?
                                    AND cantidad >= ?
                                    """;

                            try (PreparedStatement descontar =
                                    conn.prepareStatement(sqlDescontar)) {

                                descontar.setLong(1, necesario);
                                descontar.setInt(2, idMaterial);
                                descontar.setLong(3, necesario);

                                if (descontar.executeUpdate() != 1) {
                                    throw new SQLException(
                                            "Stock insuficiente para el material ID "
                                                    + idMaterial);
                                }
                            }
                        }
                    }
                }

                if (!hayMateriales) {
                    throw new SQLException(
                            "El producto no tiene una receta de fabricación.");
                }

                String sqlExisteStock = """
                        SELECT producto_id
                        FROM existencias
                        WHERE producto_id = ?
                        FOR UPDATE
                        """;

                boolean existeStock;

                try (PreparedStatement ps =
                        conn.prepareStatement(sqlExisteStock)) {

                    ps.setInt(1, idProducto);

                    try (ResultSet rs = ps.executeQuery()) {
                        existeStock = rs.next();
                    }
                }

                if (existeStock) {
                    String sqlSumar = """
                            UPDATE existencias
                            SET cantidad = cantidad + ?
                            WHERE producto_id = ?
                            """;

                    try (PreparedStatement ps =
                            conn.prepareStatement(sqlSumar)) {

                        ps.setInt(1, cantidad);
                        ps.setInt(2, idProducto);
                        ps.executeUpdate();
                    }

                } else {
                    String sqlInsertarStock = """
                            INSERT INTO existencias
                            (producto_id, cantidad)
                            VALUES (?, ?)
                            """;

                    try (PreparedStatement ps =
                            conn.prepareStatement(sqlInsertarStock)) {

                        ps.setInt(1, idProducto);
                        ps.setInt(2, cantidad);
                        ps.executeUpdate();
                    }
                }

                String sqlFinalizar = """
                        UPDATE ordenes_fabricacion
                        SET estado = 'finalizada',
                            fecha_finalizacion = CURRENT_TIMESTAMP
                        WHERE id = ? AND estado = 'en proceso'
                        """;

                try (PreparedStatement ps =
                        conn.prepareStatement(sqlFinalizar)) {

                    ps.setInt(1, id);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo finalizar la orden.");
                    }
                }

                conn.commit();

                System.out.println(
                        "Orden finalizada correctamente.");
                System.out.println(
                        "Materiales descontados y stock actualizado.");

            } catch (SQLException e) {
                conn.rollback();

                System.out.println(
                        "Fabricación cancelada: " + e.getMessage());
                System.out.println(
                        "Se han revertido los cambios de esta operación.");

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error de base de datos: " + e.getMessage());
        }
    }

    // 5. Cancelar una orden pendiente
    private void cancelarOrden(Scanner sc) {
        System.out.println("\n====== CANCELAR ORDEN ======");

        int id = Main.leerInt(sc, "ID de la orden: ");

        String confirmacion = Main.leerString(
                sc,
                "¿Seguro que quieres cancelar la orden? (y/n): ");

        if (!confirmacion.equalsIgnoreCase("y")) {
            System.out.println("Operación cancelada.");
            return;
        }

        String sql = """
                UPDATE ordenes_fabricacion
                SET estado = 'cancelada'
                WHERE id = ? AND estado = 'pendiente'
                """;

        try (Connection conn =
                DriverManager.getConnection(url, usuario, contraseña);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            if (ps.executeUpdate() == 1) {
                System.out.println(
                        "Orden cancelada correctamente.");
            } else {
                System.out.println(
                        "No existe la orden o ya se ha iniciado.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al cancelar: " + e.getMessage());
        }
    }
}
