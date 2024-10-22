package tn.esprit.spring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.entities.Contrat;
import tn.esprit.spring.entities.Employe;

public class ContratTest {

	@InjectMocks
	private Contrat contrat;

	@Mock
	private Employe mockEmploye;

	private Date testDate;

	@BeforeEach
	public void setUp() {
		// Initialize Mockito annotations
		MockitoAnnotations.initMocks(this);

		// Set up a test date and create a Contrat object
		testDate = new Date();
		contrat = new Contrat(testDate, "CDI", 2500.0f);
	}

	@Test
	public void testConstructorAndGetters() {
		assertEquals(testDate, contrat.getDateDebut());
		assertEquals("CDI", contrat.getTypeContrat());
		assertEquals(2500.0f, contrat.getSalaire());
	}

	@Test
	public void testSetDateDebut() {
		Date newDate = new Date();
		contrat.setDateDebut(newDate);
		assertEquals(newDate, contrat.getDateDebut());
	}

	@Test
	public void testSetTypeContrat() {
		contrat.setTypeContrat("CDD");
		assertEquals("CDD", contrat.getTypeContrat());
	}

	@Test
	public void testSetSalaire() {
		contrat.setSalaire(3000.0f);
		assertEquals(3000.0f, contrat.getSalaire());
	}

	@Test
	public void testSetReference() {
		contrat.setReference(12345);
		assertEquals(12345, contrat.getReference());
	}

	@Test
	public void testEmployeAssociation() {
		// Mock the employe object
		when(mockEmploye.getNom()).thenReturn("John Doe");

		contrat.setEmploye(mockEmploye);

		// Verify that the mock employe was set correctly
		assertNotNull(contrat.getEmploye());
		assertEquals("John Doe", contrat.getEmploye().getNom());
	}

	@Test
	public void testNoArgsConstructor() {
		Contrat emptyContrat = new Contrat();
		assertNotNull(emptyContrat);
	}
}

