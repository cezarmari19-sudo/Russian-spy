"""
Sistem de conturi si prieteni - VERSIUNE TEMPORARA, IN MEMORIE.

ATENTIE: la fel ca restul serverului, tot ce e aici traieste doar in RAM si se
pierde COMPLET la fiecare repornire a serverului (Render reporneste des pe
planul gratuit dupa inactivitate). Cand se adauga autentificarea reala prin
email + baza de date persistenta (task separat, mai mare), acest fisier va fi
inlocuit sa citeasca/scrie din acea baza de date, dar interfata catre main.py
(functiile de mai jos) ar trebui sa ramana aproape identica.

Conceptul de "cont" aici e legat de un accountId generat o singura data pe
telefon si salvat local (PlayerPrefs) - NU e acelasi lucru cu player_id-ul
folosit in timpul unei partide (acela e efemer, regenerat la fiecare joc nou).
"""

import random
import string
from dataclasses import dataclass, field
from typing import Optional


def _generate_friend_code() -> str:
    """Cod de 7 caractere alfanumerice, cu majuscule (fara caractere ambigue
    precum 0/O sau 1/I/L), la fel ca stilul codurilor de camera."""
    chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    return "".join(random.choices(chars, k=7))


@dataclass
class Account:
    account_id: str
    display_name: str
    friend_code: str
    # Multimi de account_id - simplu si suficient pentru volumul actual.
    friends: set[str] = field(default_factory=set)
    # Cereri de prietenie PRIMITE de acest cont, neinca acceptate/refuzate.
    # Fiecare element e account_id-ul celui care a trimis cererea.
    incoming_requests: set[str] = field(default_factory=set)

    def to_public_dict(self):
        return {
            "accountId": self.account_id,
            "displayName": self.display_name,
            "friendCode": self.friend_code,
        }


class AccountManager:
    def __init__(self):
        self.accounts: dict[str, Account] = {}
        self._codes_in_use: dict[str, str] = {}  # friend_code -> account_id

    def get_or_create_account(self, account_id: str, display_name: str) -> Account:
        existing = self.accounts.get(account_id)
        if existing is not None:
            # Actualizam numele afisat, in caz ca jucatorul l-a schimbat din Setari.
            existing.display_name = display_name
            return existing

        code = self._generate_unique_code()
        account = Account(account_id=account_id, display_name=display_name, friend_code=code)
        self.accounts[account_id] = account
        self._codes_in_use[code] = account_id
        return account

    def _generate_unique_code(self) -> str:
        while True:
            code = _generate_friend_code()
            if code not in self._codes_in_use:
                return code

    def regenerate_code(self, account_id: str) -> tuple[Optional[str], Optional[str]]:
        account = self.accounts.get(account_id)
        if account is None:
            return None, "Cont inexistent"
        old_code = account.friend_code
        new_code = self._generate_unique_code()
        account.friend_code = new_code
        self._codes_in_use.pop(old_code, None)
        self._codes_in_use[new_code] = account_id
        return new_code, None

    def set_custom_code(self, account_id: str, desired_code: str) -> tuple[bool, Optional[str]]:
        desired_code = desired_code.strip().upper()
        if len(desired_code) != 7:
            return False, "Codul trebuie sa aiba exact 7 caractere"
        if not desired_code.isalnum():
            return False, "Codul poate contine doar litere si cifre"

        account = self.accounts.get(account_id)
        if account is None:
            return False, "Cont inexistent"

        existing_owner = self._codes_in_use.get(desired_code)
        if existing_owner is not None and existing_owner != account_id:
            return False, "Codul este deja folosit de altcineva"

        old_code = account.friend_code
        account.friend_code = desired_code
        self._codes_in_use.pop(old_code, None)
        self._codes_in_use[desired_code] = account_id
        return True, None

    def send_friend_request(self, from_account_id: str, target_code: str) -> tuple[bool, Optional[str]]:
        from_account = self.accounts.get(from_account_id)
        if from_account is None:
            return False, "Cont inexistent"

        target_code = target_code.strip().upper()
        target_account_id = self._codes_in_use.get(target_code)
        if target_account_id is None:
            return False, "Niciun cont nu are acest cod"
        if target_account_id == from_account_id:
            return False, "Nu te poti adauga singur"

        target_account = self.accounts[target_account_id]
        if target_account_id in from_account.friends:
            return False, "Sunteti deja prieteni"
        if from_account_id in target_account.incoming_requests:
            return False, "Cerere deja trimisa"

        target_account.incoming_requests.add(from_account_id)
        return True, None

    def respond_to_request(self, account_id: str, requester_account_id: str, accept: bool) -> Optional[str]:
        account = self.accounts.get(account_id)
        if account is None:
            return "Cont inexistent"
        if requester_account_id not in account.incoming_requests:
            return "Nu exista o cerere de la acest cont"

        account.incoming_requests.discard(requester_account_id)

        if accept:
            requester_account = self.accounts.get(requester_account_id)
            account.friends.add(requester_account_id)
            if requester_account is not None:
                requester_account.friends.add(account_id)
        return None

    def remove_friend(self, account_id: str, friend_account_id: str) -> Optional[str]:
        account = self.accounts.get(account_id)
        if account is None:
            return "Cont inexistent"
        account.friends.discard(friend_account_id)
        friend_account = self.accounts.get(friend_account_id)
        if friend_account is not None:
            friend_account.friends.discard(account_id)
        return None

    def get_account(self, account_id: str) -> Optional[Account]:
        return self.accounts.get(account_id)

    def list_friends(self, account_id: str) -> list[Account]:
        account = self.accounts.get(account_id)
        if account is None:
            return []
        return [self.accounts[fid] for fid in account.friends if fid in self.accounts]

    def list_incoming_requests(self, account_id: str) -> list[Account]:
        account = self.accounts.get(account_id)
        if account is None:
            return []
        return [self.accounts[rid] for rid in account.incoming_requests if rid in self.accounts]


# instanta globala, folosita de main.py
account_manager = AccountManager()