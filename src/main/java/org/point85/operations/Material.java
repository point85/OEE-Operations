package org.point85.operations;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

public class Project {
	private String name;
	private int hoursDone;
	private LocalDate lastModified;
	
	private Collection<Project> subProjects = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Project(String name, int hoursDone) {
		this.name = name;
		this.hoursDone = hoursDone;
		this.lastModified = LocalDate.now();
	}

	public int getHoursDone() {
		return hoursDone;
	}

	public void setHoursDone(int hoursDone) {
		this.hoursDone = hoursDone;
	}

	public LocalDate getLastModified() {
		return lastModified;
	}

	public void setLastModified(LocalDate lastModified) {
		this.lastModified = lastModified;
	}

	public Collection<Project> getSubProjects() {
		return subProjects;
	}
}
