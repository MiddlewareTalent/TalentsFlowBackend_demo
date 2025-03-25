package com.accesshr.emsbackend.Repo.Holiday;

import org.springframework.data.jpa.repository.JpaRepository;

import com.accesshr.emsbackend.Entity.Holiday;

public interface HolidayRepo extends JpaRepository<Holiday, Integer> {


	void deleteAllById(int id);

	Holiday getAllById(int id);
	
}
