package models;

import jakarta.persistence.*;

@Entity
@Table(name="incometypes")
public class IncomeType {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="type_id", nullable = false)
	private Integer typeId;
	
	@Column(name="type_name", nullable = false)
    private String typeName;


}
