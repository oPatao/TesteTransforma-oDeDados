package Gabriel.Pereira;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PDFparaCSV {

    private static final String[] COLUNAS = {"PROCEDIMENTO", "RN", "VIGÊNCIA",
            "ODONTOLOGICA", "AMBULATORIAL", "HCO", "HSO", "REF", "PAC",
            "DUT", "SUBGRUPO", "GRUPO", "CAPÍTULO"};

    public static void main(String[] args) {
        String pdfPath = "Anexo_I_Rol_2021RN_465.2021_RN627L.2024.pdf";
        String csvPath = "dados_tabela.csv";
        String zipPath = "Teste_{Gabriel_Pereira}.zip";

        try {
            extrairDadosParaCSV(pdfPath, csvPath);
            compactarArquivoCSV(csvPath, zipPath);
            System.out.println("Extração e compactação concluídas com sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extrairDadosParaCSV(String pdfPath, String csvPath) throws IOException {
        try (PDDocument pdfDoc = PDDocument.load(new File(pdfPath));
             CSVWriter csvWriter = new CSVWriter(new FileWriter(csvPath))) {

            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(3);
            String texto = pdfStripper.getText(pdfDoc);
            String[] linhas = texto.split("\\R");

            List<String[]> resultado = new ArrayList<>();
            resultado.add(COLUNAS);

            Pattern colunasCheck = Pattern.compile("\\b(OD|AMB|HCO|HSO|REF|PAC|DUT)\\b");
            String procedimentoAtual = "";
            List<String> campos;

            for (String linha : linhas) {
                linha = linha.trim();

                if (linha.isEmpty() || linha.contains("Rol de Procedimentos") || linha.startsWith("Legenda") || linha.startsWith("RN (alteração)") || linha.contains("vigente a partir")) {
                    continue;
                }

                Matcher matcher = colunasCheck.matcher(linha);
                campos = new ArrayList<>();

                if (matcher.find()) {
                    if (!procedimentoAtual.isEmpty()) {
                        resultado.add(montarLinha(procedimentoAtual));
                        procedimentoAtual = "";
                    }
                    procedimentoAtual += linha;
                } else {
                    procedimentoAtual += " " + linha;
                }
            }


            if (!procedimentoAtual.isEmpty()) {
                resultado.add(montarLinha(procedimentoAtual));
            }

            csvWriter.writeAll(resultado);
        }
    }

    private static String[] montarLinha(String linhaCompleta) {
        String[] colunasFinais = new String[13];


        for (int i = 0; i < 13; i++) colunasFinais[i] = "";

        colunasFinais[3] = linhaCompleta.contains("OD") ? "Seg. Odontológica" : "";
        colunasFinais[4] = linhaCompleta.contains("AMB") ? "Seg. Ambulatorial" : "";
        colunasFinais[5] = linhaCompleta.contains("HCO") ? "Seg. Hospitalar Com Obstetrícia" : "";
        colunasFinais[6] = linhaCompleta.contains("HSO") ? "Seg. Hospitalar Sem Obstetrícia" : "";
        colunasFinais[7] = linhaCompleta.contains("REF") ? "Plano Referência" : "";
        colunasFinais[8] = linhaCompleta.contains("PAC") ? "Procedimento de Alta Complexidade" : "";

        Matcher m = Pattern.compile("(\\d{3}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4})").matcher(linhaCompleta);
        if (m.find()) {
            colunasFinais[1] = m.group(1);
            colunasFinais[2] = m.group(2);
        }

        Matcher mDut = Pattern.compile("DUT\\s*(\\d+)").matcher(linhaCompleta);
        if (mDut.find()) {
            colunasFinais[9] = mDut.group(1);
        }

        linhaCompleta = linhaCompleta.replaceAll("\\b(OD|AMB|HCO|HSO|REF|PAC|DUT(\\s*\\d+)?)\\b", "")
                .replaceAll("\\d{3}/\\d{4}\\s+\\d{2}/\\d{2}/\\d{4}", "")
                .trim();

        String[] restantes = linhaCompleta.split("\\s{2,}");
        colunasFinais[0] = restantes.length > 0 ? restantes[0].trim() : "";
        colunasFinais[10] = restantes.length > 1 ? restantes[1].trim() : "";
        colunasFinais[11] = restantes.length > 2 ? restantes[2].trim() : "";
        colunasFinais[12] = restantes.length > 3 ? restantes[3].trim() : "";

        return colunasFinais;
    }

    private static void compactarArquivoCSV(String csvPath, String zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry(csvPath));
            byte[] bytes = Files.readAllBytes(Paths.get(csvPath));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
    }
}