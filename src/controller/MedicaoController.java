package controller;

import java.io.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

import org.json.simple.*;
import org.json.simple.parser.*;

import model.*;
import utils.APIConsumer;
import view.TelaInicial;

/**
 * Classe que baixa, salva e carrega as medicoes dos paises.
 * @author Raphael Mesquita - 118.020.104
 *
 */
public class MedicaoController {
	/**
	 * Instancia unica da classe MedicaoController
	 */
	private static final MedicaoController medicaoController = new MedicaoController();

	private List<Medicao> confirmados;
	private List<Medicao> mortos;
	private List<Medicao> recuperados;

	/**
	 * Metodo construtor. Carrega as medicoes dos paises.
	 */
	private MedicaoController() {
		this.carregaMedicoes();
	}

	/**
	 * Metodo que retorna a instancia unica da classe MedicaoController.
	 * @return A instancia unica da classe MedicaoController.
	 */
	public static MedicaoController getInstance() {
		return MedicaoController.medicaoController;
	}

	/**
	 *	Metodo que carrega as medicoes dos paises de todos os dias atraves da classe APIConsumer.
	 *<p>
	 *	Primeiro, eh verificado se existe um cache para as medicoes e se sao medicoes do dia atual.
	 *	Se a condicao for satisfeita, atribui as medicoes do cache as listas medicoes.
	 *	Caso contrario, baixa as medicoes da api, sinalizando o progresso do download.
	 *</p>
	 */
	private void carregaMedicoes() {
		File pasta = new File("cache/medicoes");
		if(!pasta.exists()) pasta.mkdirs();
		
		File confirmadosFile = new File("cache/medicoes/" + LocalDate.now() + " - confirmados.ser");
		File mortosFile = new File("cache/medicoes/" + LocalDate.now() + " - mortos.ser");
		File recuperadosFile = new File("cache/medicoes/" + LocalDate.now() + " - recuperados.ser");
		
		String ultimoDownload = mortosFile.getName().split(" ")[0];

		if (confirmadosFile.isFile() && mortosFile.isFile() && recuperadosFile.isFile() && ultimoDownload.equals(LocalDate.now().toString())) {
			TelaInicial.barra.setVisible(false);
			System.out.println("Carregando dados ja baixados.");
			this.setConfirmados(deserialize(confirmadosFile));
			System.out.println("Medicoes de casos confirmados carregados.");
			this.setMortos(deserialize(mortosFile));
			System.out.println("Medicoes de mortos carregados.");
			this.setRecuperados(deserialize(recuperadosFile));
			System.out.println("Medicoes de recuperados carregados.");
		}

		else {
			List<Pais> paises = PaisController.getInstance().getPaises();

			System.out.println("Baixando medicoes dos paises...");
			long inicio = new Date().getTime();

			List<Medicao> confirmados = new ArrayList<>();
			List<Medicao> mortos = new ArrayList<>();
			List<Medicao> recuperados = new ArrayList<>();
			
			int total = paises.size();
			
			for (Pais pais : paises) {
				HttpResponse<String> response = APIConsumer
						.httpGet(String.format("https://api.covid19api.com/total/country/%s", pais.getSlug()));

				try {
					JSONArray resArray = (JSONArray) new JSONParser().parse(response.body());

					for (Object o : resArray) {
						JSONObject medicao = (JSONObject) o;

						LocalDateTime momento = LocalDateTime.parse(((String) medicao.get("Date")).replace("Z", ""));
						confirmados.add(new Medicao(pais, momento, (int) ((long) medicao.get("Confirmed")),
								StatusCaso.CONFIRMADOS));
						mortos.add(new Medicao(pais, momento, (int) ((long) medicao.get("Deaths")), StatusCaso.MORTOS));
						recuperados.add(new Medicao(pais, momento, (int) ((long) medicao.get("Recovered")),
								StatusCaso.RECUPERADOS));

					}
				} catch (ParseException e) {
					System.err.println("Problemas ao converter JSON em objeto.");
				}
				System.out.println(pais.getSlug());
				
				int baixado = paises.indexOf(pais) + 1;

				int porcentagem = (int) (baixado / ((float) total) * 100);
				
				TelaInicial.barra.updateBarMedicoes(baixado, total, porcentagem);
				
				System.out.printf("Progresso %d/%d(%d%%) de medicoes de paises.\n", baixado, total, porcentagem);
			}

			this.setConfirmados(confirmados);
			this.setMortos(mortos);
			this.setRecuperados(recuperados);
			
			serialize(confirmadosFile, this.getConfirmados());
			serialize(mortosFile, this.getMortos());
			serialize(recuperadosFile, this.getRecuperados());

			TelaInicial.barra.updateBarMedicoes(total, total, 100);
			
			long fim = new Date().getTime();
			float duracao = (float) (fim - inicio) / 1000.0f;
			
			System.out.printf("Medicoes dos paises baixados em %.2f segundos\n", duracao);
		}
	}

	private <T> void serialize(File file, List<T> objects) {
		try {
			FileOutputStream fileOut = new FileOutputStream(file);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(objects);
			out.close();
			fileOut.close();
			System.out.println("\nSerializalizacao realizada com sucesso\n");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problema ao abrir ou fechar o arquivo.");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Medicao> deserialize(File file) {
		ArrayList<Medicao> medicoes = new ArrayList<>();

		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			medicoes = (ArrayList<Medicao>) in.readObject();
			in.close();
			fileIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return medicoes;
	}
	
	/**
	 * Getter da lista de medicoes do numero de casos confirmados.
	 * @return A lista de medicoes do numero de casos confirmados.
	 */
	public List<Medicao> getConfirmados() {
		return new ArrayList<Medicao>(this.confirmados);
	}

	/**
	 * Setter da lista de medicoes do numero de casos confirmados.
	 * @param confirmados A lista de medicoes do numero de casos confirmados a se definir.
	 */
	public void setConfirmados(List<Medicao> confirmados) {
		this.confirmados = new ArrayList<Medicao>(confirmados);
	}

	/**
	 * Getter da lista de medicoes do numero de mortos.
	 * @return A lista de medicoes do numero de mortos.
	 */
	public List<Medicao> getMortos() {
		return new ArrayList<Medicao>(this.mortos);
	}

	/**
	 * Setter da lista de medicoes do numero de mortos.
	 * @param mortos A lista de medicoes do numero de mortos a se definir.
	 */
	public void setMortos(List<Medicao> mortos) {
		this.mortos = new ArrayList<Medicao>(mortos);
	}

	/**
	 * Getter da lista de medicoes do numero de recuperados.
	 * @return A lista de medicoes do numero de recuperados.
	 */
	public List<Medicao> getRecuperados() {
		return new ArrayList<Medicao>(this.recuperados);
	}

	/**
	 * Setter da lista de medicoes do numero de recuperados.
	 * @param recuperados A lista de medicoes do numero de recuperados a se definir.
	 */
	public void setRecuperados(List<Medicao> recuperados) {
		this.recuperados = new ArrayList<Medicao>(recuperados);
	}

}