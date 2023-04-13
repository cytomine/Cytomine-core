package be.cytomine.domain.security;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Getter
@Setter
public class AuthWithToken {

    @Id
    @GeneratedValue(generator = "myGenerator")
    private Long id;

    @Version
    protected Integer version = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    private Date expiryDate;

    String tokenKey;

    public boolean isValid(){
        return expiryDate.toInstant().isAfter(new Date().toInstant());
    }

}
