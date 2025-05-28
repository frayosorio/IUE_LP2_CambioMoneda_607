package servicios;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import entidades.CambioMoneda;

public class CambioMonedaServicio {

    public static List<CambioMoneda> getDatos(String nombreArchivo) {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("d/M/yyyy");
        try {
            Stream<String> lineas = Files.lines(Paths.get(nombreArchivo));
            return lineas.skip(1)
                    .map(linea -> linea.split(","))
                    .map(textos -> new CambioMoneda(textos[0],
                            LocalDate.parse(textos[1], formatoFecha),
                            Double.parseDouble(textos[2])))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            System.out.println(ex);
            return Collections.emptyList();
        }
    }

    public static List<String> getMonedas(List<CambioMoneda> datos) {
        return datos.stream()
                .map(CambioMoneda::getMoneda)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<CambioMoneda> filtrarCambiosMoneda(String moneda,
            LocalDate desde, LocalDate hasta,
            List<CambioMoneda> datos) {
        return datos.stream()
                .filter(item -> item.getMoneda().equals(moneda)
                        && !(item.getFecha().isBefore(desde) || item.getFecha().isAfter(hasta)))
                .collect(Collectors.toList());
    }

    public static List<LocalDate> getFechas(List<CambioMoneda> datos) {
        return datos.stream()
                .sorted(Comparator.comparing(CambioMoneda::getFecha))
                .map(CambioMoneda::getFecha)
                .collect(Collectors.toList());
    }

    public static List<Double> getCambios(List<CambioMoneda> datos) {
        return datos.stream()
                .sorted(Comparator.comparing(CambioMoneda::getFecha))
                .map(CambioMoneda::getCambio)
                .collect(Collectors.toList());
    }

    public static TimeSeriesCollection getDatosGrafica(List<LocalDate> fechas,
            List<Double> cambios, String moneda) {
        var serie = new TimeSeries("Cambios de la moneda " + moneda);

        IntStream.range(0, fechas.size())
                .forEach(i -> {
                    var fecha = fechas.get(i);
                    serie.add(new Day(fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear()),
                            cambios.get(i));
                });
        var datosGrafica = new TimeSeriesCollection();
        datosGrafica.addSeries(serie);
        return datosGrafica;
    }

    public static JFreeChart getGrafica(TimeSeriesCollection datosGrafica,
            String moneda, LocalDate desde, LocalDate hasta) {
        return ChartFactory.createTimeSeriesChart("Cambios de la moneda " + moneda + " entre " + desde + " y " + hasta,
                "Fechas", "Valor Cambio",
                datosGrafica);
    }

    public static void mostrarGrafica(JPanel pnl, JFreeChart grafica) {
        pnl.removeAll();
        var pnlGrafica = new ChartPanel(grafica);
        pnlGrafica.setPreferredSize(new Dimension(pnl.getWidth(), pnl.getHeight()));
        pnlGrafica.setMouseWheelEnabled(true);
        pnl.setLayout(new BorderLayout());
        pnl.add(pnlGrafica, BorderLayout.CENTER);
        pnl.validate();
    }

    public static double getPromedio(List<Double> datos) {
        return datos.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    public static double getDesviacion(List<Double> datos) {
        var promedio = getPromedio(datos);
        return Math.sqrt(datos.stream()
                .mapToDouble(dato -> Math.pow(dato - promedio, 2))
                .average()
                .orElse(0));
    }

    public static double getMaximo(List<Double> datos) {
        return datos.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);
    }

    public static double getMinimo(List<Double> datos) {
        return datos.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0);
    }

    public static double getMediana(List<Double> datos) {
        var datosOrdenados = datos.stream()
                .sorted()
                .collect(Collectors.toList());
        var n = datosOrdenados.size();
        return n % 2 == 0 ? (datosOrdenados.get(n / 2) + datosOrdenados.get(n / 2 - 1)) / 2 : datosOrdenados.get(n / 2);
    }

    public static double getModa(List<Double> datos) {
        return datos.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0.0);
    }

    public static Map<String, Double> getEstadisticas(String moneda,
            LocalDate desde, LocalDate hasta,
            List<CambioMoneda> datos) {

        var datosFiltrados = filtrarCambiosMoneda(moneda, desde, hasta, datos);
        var cambios=getCambios(datosFiltrados);

        Map<String, Double> estadisticas=new HashMap<>();
        estadisticas.put("Promedio", getPromedio(cambios));
        estadisticas.put("Desviación", getDesviacion(cambios));
        estadisticas.put("Máximo", getMaximo(cambios));
        estadisticas.put("Mínimo", getMinimo(cambios));
        estadisticas.put("Mediana", getMediana(cambios));
        estadisticas.put("Moda", getModa(cambios));

        return estadisticas;
    }

}
